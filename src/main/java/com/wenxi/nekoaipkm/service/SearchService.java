package com.wenxi.nekoaipkm.service;

import com.wenxi.nekoaipkm.model.vo.SearchResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 语义检索服务，直接调用 PgVector 向量存储来完成相似度搜索
 */
@Service
@RequiredArgsConstructor
public class SearchService {

    /**
     * 基于 PgVector 的向量存储
     */
    private final VectorStore vectorStore;

    /**
     * 默认返回条数
     */
    @Value("${pkm.rag.top-k:5}")
    private Integer defaultTopK;

    /**
     * 相似度阈值
     */
    @Value("${pkm.rag.similarity-threshold:0.70}")
    private Double similarityThreshold;

    /**
     * 根据自然语言查询检索相关笔记文本块
     *
     * @param query 查询内容
     * @param topK  返回数量，空值则使用默认数值（配置）
     * @return  相似文本块列表
     */
    public List<SearchResultResponse> search(String query, Integer topK) {
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(Optional.ofNullable(topK).orElse(defaultTopK))
                .similarityThreshold(similarityThreshold)
                .build();
        return vectorStore.similaritySearch(request)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 将 Spring AI Document 转换为接口响应对象
     *
     * @param document  检索命中的文本块
     * @return  接口响应对象
     */
    private SearchResultResponse toResponse(Document document) {
        return new SearchResultResponse(
                document.getText(),
                document.getScore(),
                document.getMetadata()
        );
    }

}
