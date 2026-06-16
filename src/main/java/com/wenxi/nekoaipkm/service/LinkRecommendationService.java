package com.wenxi.nekoaipkm.service;


import com.wenxi.nekoaipkm.mapper.NoteLinkMapper;
import com.wenxi.nekoaipkm.mapper.NoteMapper;
import com.wenxi.nekoaipkm.model.entity.Note;
import com.wenxi.nekoaipkm.model.entity.NoteLink;
import com.wenxi.nekoaipkm.model.vo.LinkCandidateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 笔记链接推荐服务，用语义检索发现相关旧笔记
 *
 */
@Service
@RequiredArgsConstructor
public class LinkRecommendationService {

    private final NoteMapper noteMapper;

    private final NoteLinkMapper noteLinkMapper;

    private final VectorStore vectorStore;

    private final JdbcTemplate jdbcTemplate;

    /**
     * 为指定笔记推荐相关笔记并保存推荐结果
     *
     * @param noteId
     * @return
     */
    @Transactional
    public List<LinkCandidateResponse> recommendLinks(String noteId) {
        Note sourceNote = noteMapper.selectById(noteId);
        if (sourceNote == null) {
            throw new IllegalStateException("笔记不存在：" + noteId);
        }

        String sourceContent = loadNoteContentFromVectorStore(noteId);
        SearchRequest request = SearchRequest.builder()
                .query(sourceContent)
                .topK(10)
                .similarityThreshold(0.72)
                .build();

        Map<String, LinkCandidateResponse> candidates = new LinkedHashMap<>();

        for (Document document : vectorStore.similaritySearch(request)) {
            String targetNoteId = String.valueOf(
                    document.getMetadata().get("note_id")
            );

            // 不能把笔记自己推荐给自己
            if (noteId.equals(targetNoteId)) {
                continue;
            }

            String targetTitle = String.valueOf(
                    document.getMetadata().get("title")
            );
            double confidence = document.getScore() == null ? 0.0 : document.getScore();

            candidates.putIfAbsent(
                    targetNoteId,
                    new LinkCandidateResponse(
                            targetNoteId,
                            targetTitle,
                            confidence,
                            "语义内容相似，建议建立双向链接。"

                    )
            );
        }

        List<LinkCandidateResponse> responses = candidates.values()
                .stream()
                .limit(5)
                .toList();

        saveLinks(noteId, responses);
        return responses;

    }


    /**
     * 从 vector_store 聚合某个笔记的所有文件夹
     * @param noteId
     * @return
     */
    private String loadNoteContentFromVectorStore(String noteId) {
        String sql = """
                select coalesce(string_agg(content, E'\\n\\n'), '')
                from vector_store
                where metadata ->> 'note_id' = ?
                """;

        String content = jdbcTemplate.queryForObject(sql, String.class, noteId);
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("笔记还没有向量化：" + noteId);
        }
        return content;
    }

    /**
     * 保存链接推荐结果
     * 
     * @param sourceNoteId
     * @param responses
     */
    private void saveLinks(String sourceNoteId, List<LinkCandidateResponse> responses) {
        for (LinkCandidateResponse response : responses) {
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
}
