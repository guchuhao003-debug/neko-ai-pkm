package com.wenxi.nekoaipkm.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 笔记语义关联实体，用于保存双向链接推荐结果
 */

@Data
@TableName("note_link")
public class NoteLink {

    /**
     * 数据库自增主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 发起推荐的笔记 ID
     */
    private String sourceNoteId;

    /**
     * 被推荐关联的目标笔记 ID
     */
    private String targetNoteId;

    /**
     * 关联置信度，数值越高表示越相关
     */
    private Double confidence;

    /**
     * 推荐理由（首版存放简短说明）
     */
    private String reason;

    /**
     * 推荐记录的创建时间
     */
    private LocalDateTime createdAt;

}
