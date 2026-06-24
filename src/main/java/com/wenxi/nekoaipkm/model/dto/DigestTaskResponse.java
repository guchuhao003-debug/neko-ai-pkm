package com.wenxi.nekoaipkm.model.dto;

/**
 * 周报任务提交响应
 *
 * @param taskId    任务 ID
 * @param status    当前任务状态
 * @param message   提示信息
 */
public record DigestTaskResponse(
        String taskId,
        String status,
        String message
) {
}
