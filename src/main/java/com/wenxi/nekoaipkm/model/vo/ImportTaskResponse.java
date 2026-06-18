package com.wenxi.nekoaipkm.model.vo;

/**
 * 导入任务提交响应。
 *
 * @param taskId    任务 ID
 * @param status    任务状态
 * @param message   提示信息
 */
public record ImportTaskResponse(
        String taskId,
        String status,
        String message
){}
