package com.wenxi.nekoaipkm.model.vo;

/**
 * 导入任务状态响应。
 *
 * @param taskId 任务 ID
 * @param status 任务状态
 * @param result 导入结果，任务完成后才有值
 * @param errorMessage 错误信息，任务失败后才有值
 */
public record ImportTaskStatusResponse(
        String taskId,
        String status,
        ImportResponse result,
        String errorMessage
) {

    /**
     * 创建运行中任务状态。
     *
     * @param taskId 任务 ID
     * @return 运行中状态
     */
    public static ImportTaskStatusResponse running(String taskId) {
        return new ImportTaskStatusResponse(taskId,"running",null,null);
    }

    /**
     * 创建成功任务状态
     *
     * @param taskId    任务 ID
     * @param result    导入结果
     * @return  成功状态
     */
    public static ImportTaskStatusResponse success(String taskId, ImportResponse result) {
        return new ImportTaskStatusResponse(taskId,"success",result,null);
    }

    /**
     * 创建失败任务状态。
     *
     * @param taskId 任务 ID
     * @param errorMessage 错误信息
     * @return 失败状态
     */
    public static ImportTaskStatusResponse failed(String taskId, String errorMessage) {
        return new ImportTaskStatusResponse(taskId,"failed",null,errorMessage);
    }



}
