package com.wenxi.nekoaipkm.model.dto;

import java.time.LocalDateTime;

/**
 * 周报任务状态响应
 *
 * @param taskId    任务 ID
 * @param status    任务状态：pending / running / success / failed
 * @param digestId  生成成功后的周报 ID
 * @param message   任务状态说明
 * @param createdAt 任务创建时间
 * @param updatedAt 任务更新时间
 */
public record DigestTaskStatusResponse(
        String taskId,
        String status,
        Long digestId,
        String message,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
