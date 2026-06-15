package com.wenxi.nekoaipkm.model.vo;

import java.util.Map;

/**
 * 语义检索响应结果
 *
 * @param content   命中文本块内容
 * @param score     相似度分数
 * @param metadata  文本块元数据
 */
public record SearchResultResponse(

        /**
         * 命中文本块内容
         */
        String content,

        /**
         * 相似度分数
         */
        Double score,

        /**
         * 文本块元数据
         */
        Map<String, Object> metadata

) {
}
