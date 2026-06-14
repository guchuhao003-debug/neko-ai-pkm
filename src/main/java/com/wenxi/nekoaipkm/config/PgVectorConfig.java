package com.wenxi.nekoaipkm.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * PgVector 向量数据库配置
 * 手动创建一个 VectorStore Bean，让 Spring AI 使用 PostgreSQL + pgvector 作为向量数据库
 */

@Configuration
public class PgVectorConfig {

    /**
     * 创建 PgVectorStore Bean 提供导入、检索和 RAG 问答复用
     *
     * @param jdbcTemplate  PostgreSQL JDBC 模板
     * @param embeddingModel    DashScope Embedding 模型
     * @return  PgVectorStore Bean 向量存储实现
     */
    @Bean
    public VectorStore vectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                // 向量维度
                .dimensions(1536)
                // 使用余弦距离计算文本相似度。
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                // 使用 HNSW 向量索引
                .indexType(PgVectorStore.PgIndexType.HNSW)
                // false 表示不要让 Spring AI 自动创建 pgvector 表结构 ，索引主要由 Flyway 脚本管理创建
                .initializeSchema(false)
                // 使用 PostgreSQL 的 public schema。
                .schemaName("public")
                // 表示 Spring AI PgVectorStore 使用的向量表名
                .vectorTableName("vector_store")
                // 表示批量写入向量时，每批最多写入 1000 个 Document。
                .maxDocumentBatchSize(1000)
                .build();
    }

}
