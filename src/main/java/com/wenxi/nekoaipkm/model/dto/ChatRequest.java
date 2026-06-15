package com.wenxi.nekoaipkm.model.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * RAG 问答请求
 *
 * @param message 用户问题消息
 */
public record ChatRequest(

        @NotBlank(message = "问题消息不能为空")
        String message

) {}
