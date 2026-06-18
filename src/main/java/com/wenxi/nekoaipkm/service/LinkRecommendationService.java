package com.wenxi.nekoaipkm.service;


import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wenxi.nekoaipkm.mapper.NoteLinkMapper;
import com.wenxi.nekoaipkm.mapper.NoteMapper;
import com.wenxi.nekoaipkm.model.entity.Note;
import com.wenxi.nekoaipkm.model.entity.NoteLink;
import com.wenxi.nekoaipkm.model.vo.LinkCandidateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static com.wenxi.nekoaipkm.constant.PromptConstant.REASON_PROMPT;
import static com.wenxi.nekoaipkm.constant.PromptConstant.SUMMARY_PROMPT;

/**
 * 笔记链接推荐服务，用语义检索发现相关旧笔记
 *
 */
@Service
@RequiredArgsConstructor
public class LinkRecommendationService {

    // 推荐数量
    private static final int RECOMMEND_LIMIT = 5;

    // 向量检索候选数量（30 个），多于最终数量以便 MMR 筛选
    private static final int SEARCH_CANDIDATE_LIMIT = 30;

    // 查询文本的最大字符数
    private static final int QUERY_MAX_CHARS = 4000;

    // LLM 生成摘要的源文本最大长度
    private static final int SUMMARY_SOURCE_MAX_CHARS = 12000;

    // Markdown 结构查询的最低长度, 低于此长度则改用全文+标题
    private static final int MIN_STRUCTURE_QUERY_CHARS = 160;

    // 提取标题层级时最多取 30 个
    private static final int MAX_MARKDOWN_HEADERS = 30;

    // 相似度阈值 0.72，过滤低相关候选
    private static final double SIMILARITY_THRESHOLD = 0.72;

    // MMR 平衡因子 0.75，更偏向相关性（75%）而非多样性（25%）
    private static final double MMR_LAMBDA = 0.75;

    private final NoteMapper noteMapper;

    private final NoteLinkMapper noteLinkMapper;

    // 向量检索核心
    private final VectorStore vectorStore;

    // 向量模型，用于将文本片段向量化，供 MMR 计算余弦相似度
    private final EmbeddingModel embeddingModel;

    // ChatClient 构造器，用于调用 LLM 生成摘要和推荐理由
    private final ChatClient.Builder chatClientBuilder;

    // 执行原生 SQL 查询向量块和已有链接
    private final JdbcTemplate jdbcTemplate;

    /**
     * 同步推荐相关笔记，适合本地调试和小规模笔记库。
     *
     * @param noteId 源笔记 ID
     * @return 推荐结果列表
     */
    @Transactional
    public List<LinkCandidateResponse> recommendLinks(String noteId) {
        return doRecommendLinks(noteId);
    }

    /**
     * 异步推荐相关笔记，适合在接口中提交后台任务。
     *
     * @param noteId 源笔记 ID
     * @return 异步任务结果
     */
    @Async
    @Transactional
    public CompletableFuture<Void> recommendLinksInBackground(String noteId) {
        doRecommendLinks(noteId);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 执行链接推荐主流程
     *
     * @param noteId    源笔记 ID
     * @return  推荐结果列表
     */
    private List<LinkCandidateResponse> doRecommendLinks(String noteId) {
        Note sourceNote = noteMapper.selectById(noteId);
        if (sourceNote == null) {
            throw new IllegalStateException("笔记不存在：" + noteId);
        }

        List<String> sourceChunks = loadNoteChunksFromVectorStore(noteId);
        String sourceQuery = buildRepresentativeQuery(
                sourceNote.getTitle(),
                sourceChunks
        );

        Set<String> linkedNoteIds = findExistingLinkedNoteIds(noteId);

        SearchRequest request = SearchRequest.builder()
                .query(sourceQuery)
                .topK(SEARCH_CANDIDATE_LIMIT)
                .similarityThreshold(SIMILARITY_THRESHOLD)
                .build();

        Map<String, CandidateDocument> candidates = new LinkedHashMap<>();

        for (Document document : vectorStore.similaritySearch(request)) {
            String targetNoteId = String.valueOf(
                    document.getMetadata().get("note_id")
            );

            // 排除自己和已经建立过的双向链接，避免重复推荐
            if (noteId.equals(targetNoteId) || linkedNoteIds.contains(targetNoteId)) {
                continue;
            }

            String targetTitle = String.valueOf(
                    document.getMetadata().get("title")
            );
            double confidence = document.getScore() == null ? 0.0 : document.getScore();

            CandidateDocument candidate = new CandidateDocument(
                    targetNoteId,
                    targetTitle,
                    confidence,
                    document.getText(),
                    embeddingModel.embed(document.getText())
            );
            // 同一篇目标笔记可能命中多个块，只保留分数最高的那个块。
            candidates.merge(targetNoteId, candidate, this::pickHigherConfidence);
        }

        List<CandidateDocument> selectedCandidates = selectByMmr(
                new ArrayList<>(candidates.values()),
                RECOMMEND_LIMIT
        );
        String sourceSnippet = firstNonBlankChunk(sourceChunks);
        List<LinkCandidateResponse> responses = selectedCandidates.stream()
                .map(candidate -> toResponse(sourceSnippet, candidate))
                .toList();

        saveLinks(noteId, responses);
        return responses;

    }


    /**
     * 从 vector_store 读取某个笔记的全部文本块
     *
     * @param noteId    笔记 ID
     * @return  文本块列表
     */
    private List<String> loadNoteChunksFromVectorStore(String noteId) {
        String sql = """
                select content from vector_store
                where metadata ->> 'note_id' = ?
                order by coalesce((metadata ->> 'chunk_index')::int, 0)
                """;

        List<String> chunks = jdbcTemplate.queryForList(sql, String.class, noteId);

        if (chunks.isEmpty()) {
            throw new IllegalStateException("笔记还没有向量化：" + noteId);
        }
        return chunks;
    }

    /**
     * 构造代表性查询文本，优先使用 MarkDown 结构降低 Query 长度
     * @param title     笔记标题
     * @param chunks    笔记文本块
     * @return      代表性查询文本
     */
    private String buildRepresentativeQuery(String title, List<String> chunks) {
        String fullContent = String.join("\n\n", chunks);
        String markdownQuery = buildMarkdownStructureQuery(title,fullContent);

        if(fullContent.length() <= QUERY_MAX_CHARS) {
            if (markdownQuery.length() >= MIN_STRUCTURE_QUERY_CHARS) {
                return clip(markdownQuery, QUERY_MAX_CHARS);
            }
            return title + "\n\n" + fullContent;
        }

        String summaryQuery = buildSummaryQueryWithLlm(
                title,
                markdownQuery,
                fullContent
        );
        if (summaryQuery != null && !summaryQuery.isBlank()) {
            return clip(title + "\n\n" + summaryQuery, QUERY_MAX_CHARS);
        }

        if (!markdownQuery.isBlank()) {
            return clip(markdownQuery, QUERY_MAX_CHARS);
        }
        return buildSlidingWindowQuery(title, chunks);
    }

    /**
     * 基于 Markdown 结构生成查询文本。
     *
     * @param title     笔记标题
     * @param content   笔记全文
     * @return     Markdown 结构查询文本
     */
    private String buildMarkdownStructureQuery(String title, String content) {

        List<String> headers = extractMarkdownHeaders(content);
        String firstParagraph = extractFirstParagraph(content);

        StringBuilder builder = new StringBuilder();
        builder.append("笔记标题：").append(title).append("\n\n");

        if (!headers.isEmpty()) {
            builder.append("标题层级: \n");
            headers.forEach(header -> builder.append("-").append(header).append("\n"));
            builder.append("\n");
        }

        if (!firstParagraph.isBlank()) {
            builder.append("首段内容: \n").append(firstParagraph);
        }

        return builder.toString().trim();
    }

    /**
     * 提取 MarkDown 标题层级
     *
     * @param content   Markdown 内容
     * @return  标题列表
     */
    private List<String> extractMarkdownHeaders(String content) {
        return content.lines()
                .map(String::trim)
                .filter(line -> line.startsWith("#"))
                .filter(line -> !line.startsWith("#######"))
                .map(line -> line.replaceFirst("^#{1,6}\\s*", "").trim())
                .filter(line -> !line.isBlank())
                .limit(MAX_MARKDOWN_HEADERS)
                .toList();
    }

    /**
     * 提取 Markdown 正文首段
     * @param content
     * @return
     */
    private String extractFirstParagraph(String content) {
        StringBuilder paragraph = new StringBuilder();
        boolean inCodeBlock = false;

        for (String rawLine : content.lines().toList()) {
            String line = rawLine.trim();

            if(line.startsWith("```")) {
                inCodeBlock = !inCodeBlock;
                continue;
            }

            if(inCodeBlock || line.startsWith("#")) {
                continue;
            }

            if(line.isBlank()) {
              if(paragraph.length() > 0) {
                  break;
              }
              continue;
            }

            paragraph.append(line).append("\n");
        }
        return clip(paragraph.toString().trim(), 1200);
    }

    /**
     * 优先使用 LLM 将长笔记压缩成适合向量检索的摘要 Query。
     *
     * @param title 笔记标题
     * @param markdownQuery Markdown 结构查询文本
     * @param fullContent   笔记全文
     * @return  摘要查询文本，失败则为空
     */
    private String buildSummaryQueryWithLlm(String title, String markdownQuery,String fullContent) {

        try {
            return chatClientBuilder.build()
                    .prompt()
                    .system("你是一个专业的个人知识管理助手，请为笔记生成适合语义检索的摘要。")
                    .user(user -> user.text(SUMMARY_PROMPT)
                            .param("title",title)
                            .param("structure",markdownQuery)
                            .param("content",clip(fullContent, SUMMARY_SOURCE_MAX_CHARS))
                    )
                    .call()
                    .content();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * LLM 摘要失败时使用滑动窗口兜底
     *
     * @param title 笔记标题
     * @param chunks    笔记文本块
     * @return  兜底查询文本
     */
    private String buildSlidingWindowQuery(String title, List<String> chunks) {
        List<String> representativeChunks = new ArrayList<>();

        if (!chunks.isEmpty()) {
            representativeChunks.add(chunks.get(0));
        }

        if (chunks.size() > 2) {
            representativeChunks.add(chunks.get(chunks.size() / 2));
        }

        if (chunks.size() > 1) {
            representativeChunks.add(chunks.get(chunks.size() - 1));
        }

        String query = title + "\n\n" + String.join("\n\n", representativeChunks);
        return clip(query, QUERY_MAX_CHARS);
    }

    /**
     * 查询已经存在的双向链接目标
     *
     * @param noteId    当前笔记 ID
     * @return  已有关联笔记 ID 集合
     */
    private Set<String> findExistingLinkedNoteIds(String noteId) {
        String sql = """
                select case
                        when source_note_id = ? then target_note_id
                        else source_note_id
                end as linked_note_id
                from note_link
                where source_note_id = ? or target_note_id = ?
                """;

        return new HashSet<>(
                jdbcTemplate.queryForList(sql, String.class, noteId, noteId, noteId)
        );
    }

    /**
     * 在候选文档中选择置信度更高的一项
     *
     * @param current   当前候选
     * @param next  新候选
     * @return  置信度更高的候选项
     */
    private CandidateDocument pickHigherConfidence(CandidateDocument current, CandidateDocument next) {
        return current.confidence() >= next.confidence() ? current : next;
    }

    /**
     * 使用 MMR 从候选笔记中选择更有多样性的结果
     *
     * @param candidates    候选笔记
     * @param limit 推荐数量
     * @return  重排后的候选
     */
    private List<CandidateDocument> selectByMmr(List<CandidateDocument> candidates, int limit) {
        List<CandidateDocument> selected = new ArrayList<>();
        List<CandidateDocument> remaining = new ArrayList<>(candidates);

        while(!remaining.isEmpty() && selected.size() < limit) {
            CandidateDocument bestCandidate = remaining.stream()
                    .max(Comparator.comparingDouble(
                            candidate -> mmrScore(candidate, selected)
                    ))
                    .orElseThrow();
            selected.add(bestCandidate);
            remaining.remove(bestCandidate);
        }
        return selected;
    }

    /**
     * 计算 MMR 分数
     * @param candidate 当前候选
     * @param selected  已选择候选
     * @return  MMR 分数
     */
    private double mmrScore(CandidateDocument candidate, List<CandidateDocument> selected) {
        double relevance = candidate.confidence();
        double diversityPenalty = selected.stream()
                .mapToDouble(item -> cosineSimilarity(candidate.embedding(), item.embedding()))
                .max()
                .orElse(0.0);

        return MMR_LAMBDA * relevance - (1 - MMR_LAMBDA) * diversityPenalty;
    }

    /**
     * 计算两个向量的余弦相似度
     *
     * @param left  左侧向量
     * @param right 右侧向量
     * @return  余弦相似度
     */
    private double cosineSimilarity(float[] left, float[] right) {
        double dot = 0.0;
        double leftNorm = 0.0;
        double rightNorm = 0.0;

        for (int i = 0; i < left.length; i++) {
            dot += left[i] * right[i];
            leftNorm += left[i] * left[i];
            rightNorm += right[i] * right[i];
        }

        if (leftNorm == 0.0 || rightNorm == 0.0) {
            return 0.0;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    /**
     * 将候选文档转换为接口响应
     *
     * @param sourceSnippet 源笔记片段
     * @param candidate 候选文档
     * @return  推荐响应
     */
    private LinkCandidateResponse toResponse(String sourceSnippet, CandidateDocument candidate) {
        return  new LinkCandidateResponse(
                candidate.targetNoteId(),
                candidate.targetTitle(),
                candidate.confidence(),
                buildReason(sourceSnippet, candidate.content())
        );
    }

    /**
     * 使用 LLM 生成更具解释性的推荐理由
     *
     * @param sourceSnippet 源笔记片段
     * @param targetSnippet 目标笔记片段
     * @return  推荐理由
     */
    private String buildReason(String sourceSnippet, String targetSnippet) {
        try {
            return chatClientBuilder.build()
                    .prompt()
                    .system("你是一名专业的个人知识管理助手，请用一句话解释两篇笔记为何值得互相链接。")
                    .user(user -> user.text(REASON_PROMPT)
                            .param("source",clip(sourceSnippet, 800))
                            .param("target",clip(targetSnippet, 800))
                    )
                    .call()
                    .content();
        } catch (Exception e) {
            return "两篇笔记存在较高语义相关性，建议建立双向链接。";
        }
    }

    /**
     * 获取第一个非空文本块
     *
     * @param chunks    文本块列表
     * @return  非空文本块
     */
    private String firstNonBlankChunk(List<String> chunks) {
        return chunks.stream()
                .filter(chunk -> chunk != null && !chunk.isBlank())
                .findFirst()
                .orElse("");
    }

    /**
     * 截断文本到指定长度
     *
     * @param text  原始文本
     * @param maxLength 最大长度
     * @return  截断后的文本
     */
    private String clip(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }

    /**
     * 保存链接推荐结果
     * 
     * @param sourceNoteId  源笔记 ID
     * @param responses 推荐结果
     */
    private void saveLinks(String sourceNoteId, List<LinkCandidateResponse> responses) {
        for (LinkCandidateResponse response : responses) {
            if (hasExistingLink(sourceNoteId, response.targetNoteId())) {
                continue;
            }
            NoteLink link = new NoteLink();
            link.setSourceNoteId(sourceNoteId);
            link.setTargetNoteId(response.targetNoteId());
            link.setConfidence(response.confidence());
            link.setReason(response.reason());
            link.setCreatedAt(LocalDateTime.now());

            try {
                noteLinkMapper.insert(link);
            } catch (Exception ignored) {
                // 唯一索引会防止重复链接，重复时忽略
            }
        }
    }

    /**
     * 判断两篇笔记之间是否已经存在链接。
     *
     * @param sourceNoteId 源笔记 ID
     * @param targetNoteId 目标笔记 ID
     * @return 是否已存在链接
     */
    private boolean hasExistingLink(String sourceNoteId, String targetNoteId) {
        Long count = noteLinkMapper.selectCount(
                Wrappers.lambdaQuery(NoteLink.class)
                        .and(wrapper -> wrapper
                                .eq(NoteLink::getSourceNoteId, sourceNoteId)
                                .eq(NoteLink::getTargetNoteId, targetNoteId)
                        )
                        .or(wrapper -> wrapper
                                .eq(NoteLink::getSourceNoteId, targetNoteId)
                                .eq(NoteLink::getTargetNoteId, sourceNoteId)
                        )
        );
        return count != null && count > 0;
    }

    /**
     * MMR 使用的候选文档。
     *
     * @param targetNoteId 目标笔记 ID
     * @param targetTitle 目标笔记标题
     * @param confidence 和源笔记的相似度
     * @param content 命中的目标笔记片段
     * @param embedding 目标笔记片段向量
     */
    private record CandidateDocument(
            String targetNoteId,
            String targetTitle,
            double confidence,
            String content,
            float[] embedding
    ) {
    }
}
