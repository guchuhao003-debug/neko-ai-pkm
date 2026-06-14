package com.wenxi.nekoaipkm.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 笔记元数据实体类，用于记录每个知识文件的导入状态
 */

@Data
@TableName("note")
public class Note {

    /**
     * 笔记唯一标识
     */
    @TableId
    private String id;

    /**
     * 笔记标题，默认来自 Markdown 文件名或一级标题。
     */
    private String title;

    /**
     * 来源类型，例如 markdown、pdf、web、obsidian。
     */
    private String sourceType;

    /**
     * 原始文件路径或网页 URL。
     */
    private String sourcePath;

    /**
     * 文件内容哈希，用于判断是否需要重新向量化。
     */
    private String contentHash;

    /**
     * 笔记创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 笔记更新时间。
     */
    private LocalDateTime updatedAt;

    /**
     * 内容字符数，用于粗略观察笔记规模。
     */
    private Integer wordCount;

    /**
     * 标签字符串，第一版先用逗号分隔。
     */
    private String tags;

    /**
     * 是否已经完成向量化
     */
    private Boolean vectorized;

}
