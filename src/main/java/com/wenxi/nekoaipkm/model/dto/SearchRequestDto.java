package com.wenxi.nekoaipkm.model.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 语义检索请求体
 *
 * @param query 用户输入的问题或关键词
 * @param topK  返回的最大结果数
 */
public record SearchRequestDto(
        /**
         * 查询内容
         */
        @NotBlank(message = "查询内容不能为空")
        String query,

        /**
         * 返回结果数量，默认 10
         */
        Integer topK
) {}

