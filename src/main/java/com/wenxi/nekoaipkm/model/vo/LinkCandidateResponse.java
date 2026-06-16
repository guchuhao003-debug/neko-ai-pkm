package com.wenxi.nekoaipkm.model.vo;


/**
 * 笔记链接推荐结果
 *
 * @param targetNoteId  目标笔记 ID
 * @param targetTitle   目标笔记标题
 * @param confidence    关联置信度
 * @param reason        推荐理由
 */
public record LinkCandidateResponse(

        /**
         * 关联目标笔记的 ID
         */
        String targetNoteId,

        /**
         * 关联目标笔记的标题
         */
        String targetTitle,

        /**
         * 关联置信度
         */
        Double confidence,

        /**
         * 推荐关联理由
         */
        String reason
) {
}
