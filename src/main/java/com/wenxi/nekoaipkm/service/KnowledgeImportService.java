package com.wenxi.nekoaipkm.service;


import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wenxi.nekoaipkm.mapper.NoteMapper;
import com.wenxi.nekoaipkm.model.vo.ImportResponse;
import com.wenxi.nekoaipkm.model.entity.Note;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * 知识导入服务，负责扫描本地 MarkDown 文档并写入向量数据库
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeImportService {

    private final NoteMapper noteMapper;

    /**
     * 基于 PgVector 的向量数据库
     */
    private final VectorStore vectorStore;

    /**
     * JDBC 模板
     */
    private final JdbcTemplate jdbcTemplate;

    @Value("${pkm.notes.path:./notes}")
    private String notesBasePath;

    /**
     * 扫描本地笔记目录，增量导入发生变化的 MarkDown 文档
     * @return
     */
    @Transactional
    public ImportResponse scanAndImport() {
        Path notesDir = Paths.get(notesBasePath);
        createNotesDirectoryIfNeeded(notesDir);

        List<Path> markdownFiles = listMarkdownFiles(notesDir);
        AtomicInteger importedFiles = new AtomicInteger();
        AtomicInteger skippedFiles = new AtomicInteger();

        for (Path markdownFile : markdownFiles) {
            boolean imported = importMarkdownIfChanged(markdownFile);
            if (imported) {
                importedFiles.incrementAndGet();
            } else {
                skippedFiles.incrementAndGet();
            }
        }

        return new ImportResponse(
                markdownFiles.size(),
                importedFiles.get(),
                skippedFiles.get()
        );

    }

    /**
     * 导入单个 MarkDown 文件，如果内容没有变化则跳过
     *
     * @param markdownFile MarkDown 文件路径
     * @return  true 表示发生导入或更新， false 表示跳过
     */
    public boolean importMarkdownIfChanged(Path markdownFile) {
        String sourcePath = markdownFile.toAbsolutePath().toString();
        String content = readFileContent(markdownFile);
        String contentHash = SecureUtil.sha256(content);
        // 根据源路径获取笔记
        Note existingNote = findBySourcePath(sourcePath);
        if (existingNote != null && contentHash.equals(existingNote.getContentHash())
                && Boolean.TRUE.equals(existingNote.getVectorized())
                && hasVectorChunks(existingNote.getId())
        ) {
            log.info("笔记未变化，跳过导入: {}", sourcePath);
            return false;
        }

        Note note = saveOrUpdateNote(existingNote, markdownFile, content, contentHash);

        // 文件更新后必须先删除旧的向量块，避免同一笔记被重复检索
        deleteOldVectorChunks(note.getId());

        List<Document> chunks = readAndSplitMarkdown(markdownFile, note);
        vectorStore.add(chunks);

        note.setVectorized(true);
        note.setUpdatedAt(LocalDateTime.now());
        noteMapper.updateById(note);

        log.info("笔记导入完成：{}, 向量块数量: {}", note.getTitle(), chunks.size());
        return true;
    }


    /**
     * 创建笔记目录，避免首次运行时目录不存在
     * @param notesDir
     */
    private void createNotesDirectoryIfNeeded(Path notesDir) {
        try {
            Files.createDirectories(notesDir);
        } catch (IOException e) {
            throw new IllegalStateException("创建笔记目录失败：" + notesDir, e);
        }
    }

    /**
     * 扫描目录下所有 MarkDown 文档文件
     * @param notesDir
     * @return
     */
    private List<Path> listMarkdownFiles(Path notesDir) {
        try (Stream<Path> paths  = Files.walk(notesDir)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".md"))
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("扫描笔记目录失败：" + notesDir, e);
        }
    }

    /**
     * 按来源路径查询笔记
     * @param sourcePath
     * @return
     */
    private Note findBySourcePath(String sourcePath) {
        return noteMapper.selectOne(
                Wrappers.lambdaQuery(Note.class).eq(Note::getSourcePath, sourcePath)
        );
    }

    /**
     * 保存或更新笔记元数据
     *
     * @param existingNote
     * @param markdownFile
     * @param content
     * @param contentHash
     * @return
     */
    private Note saveOrUpdateNote(Note existingNote, Path markdownFile, String content, String contentHash) {
        LocalDateTime now = LocalDateTime.now();
        Note note = existingNote == null ? new Note() : existingNote;

        // 如果笔记为空 / 不存在
        if (existingNote == null) {
            note.setId(IdUtil.fastSimpleUUID());
            note.setCreatedAt(now);
            note.setSourceType("markdown");
            note.setSourcePath(markdownFile.toAbsolutePath().toString());
        }

        note.setTitle(resolveTitle(markdownFile, content));
        note.setContentHash(contentHash);
        note.setUpdatedAt(now);
        note.setWordCount(content.length());
        note.setVectorized(false);

        if (existingNote == null) {
            // 保存
            noteMapper.insert(note);
        } else {
            // 更新
            noteMapper.updateById(note);
        }
        return note;
    }

    /**
     * 解析笔记标题，优先使用 MarkDown 一级标题
     *
     * @param markdownFile
     * @param content
     * @return
     */
    private String resolveTitle(Path markdownFile, String content) {
        return content.lines()
                .filter(line -> line.startsWith("# "))
                .map(line -> line.substring(2).trim())
                .filter(title -> !title.isBlank())
                .findFirst()
                .orElseGet(() -> {
                    String fileName = markdownFile.getFileName().toString();
                    return fileName.replaceFirst("\\.md$", "");
                });
    }

    /**
     * 删除某个笔记旧的向量块
     *
     * @param noteId
     */
    private void deleteOldVectorChunks(String noteId) {
        jdbcTemplate.update(
                "delete from vector_store where metadata ->> 'note_id' = ?",
                noteId
        );
    }

    /**
     * 读取 MarkDown 并切分为适合向量检索的文本块
     * @param markdownFile
     * @param note
     * @return
     */
    private List<Document> readAndSplitMarkdown(Path markdownFile, Note note) {

        /**
         * 读取 MarkDown 文件内容
         */
        MarkdownDocumentReader reader = new MarkdownDocumentReader(
                new FileSystemResource(markdownFile),
                MarkdownDocumentReaderConfig.builder().build()
        );

        // ETL 流程

        List<Document> documents = reader.get();
        TokenTextSplitter splitter = new TokenTextSplitter();
        List<Document> chunks = splitter.apply(documents);

        for (int i = 0; i < chunks.size(); i++) {
            Map<String, Object> metadata = chunks.get(i).getMetadata();
            metadata.put("note_id", note.getId());
            metadata.put("title", note.getTitle());
            metadata.put("source_type", note.getSourceType());
            metadata.put("source_path", note.getSourcePath());
            metadata.put("chunk_index", i);
        }

        return chunks;
    }

    /**
     * 读取 UTF - 8 文本文件内容
     * @param filePath  文件路径
     * @return  文件内容
     */
    private String readFileContent(Path filePath) {
        try {
            return Files.readString(filePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("读取文件失败: " + filePath, e);
        }
    }

    /**
     * 是否有向量化文本块
     * @param noteId
     * @return
     */
    private boolean hasVectorChunks(String noteId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from vector_store where metadata ->> 'note_id' = ?",
                Integer.class,
                noteId
        );
        return count != null && count > 0;
    }

}

