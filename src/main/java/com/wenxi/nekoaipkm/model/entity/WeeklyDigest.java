package com.wenxi.nekoaipkm.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 周报实体类，用于保存每周知识回顾
 */

@Data
@TableName("weekly_digest")
public class WeeklyDigest {

    /**
     * 数据库自增主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 周报开始日期
     */
    private LocalDate weekStart;

    /**
     * 周报结束日期
     */
    private LocalDate weekEnd;

    /**
     * AI 生成的周报正文
     */
    private String content;

    /**
     * 邮件发送状态（首版可保存 pending）
     */
    private String sentStatus;

    /**
     * 邮件发送时间
     */
    private LocalDateTime sentAt;

    /**
     * 周报创建时间
     */
    private LocalDateTime createdAt;

}
