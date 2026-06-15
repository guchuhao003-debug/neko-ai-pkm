package com.wenxi.nekoaipkm.model.vo;

/**
 * RAG 问答响应结果
 *
 * @param answer    AI 基于本地知识库生成的回答
 */
public record ChatResponse(String answer) {
}
