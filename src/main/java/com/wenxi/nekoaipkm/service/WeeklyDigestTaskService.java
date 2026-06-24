package com.wenxi.nekoaipkm.service;

import com.wenxi.nekoaipkm.model.dto.DigestTaskStatusResponse;
import com.wenxi.nekoaipkm.model.entity.WeeklyDigest;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 周报异步任务服务，用于提交任务和查询任务状态
 */
@Service
@RequiredArgsConstructor
public class WeeklyDigestTaskService {

    private final WeeklyDigestService weeklyDigestService;

    // TODO taskStore 内存版任务状态，可以优化为数据库表 / redis
    private final Map<String, DigestTaskStatusResponse> taskStore = new ConcurrentHashMap<>();

    /**
     * 创建周报任务
     *
     * @return  任务 ID
     */
    public String createTask() {
        String taskId = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime now = LocalDateTime.now();

        taskStore.put(
                taskId,
                new DigestTaskStatusResponse(
                        taskId,
                        "pending",
                        null,
                        "周报任务已创建",
                        now,
                        now
                )
        );

        return taskId;
    }

    /**
     * 异步生成当前自然周周报。
     *
     * @param taskId 任务 ID
     * @return 异步执行结果
     */
    @Async("digestTaskExecutor")
    public CompletableFuture<Void> generateCurrentWeekDigestAsync(String taskId) {
        updateTask(taskId, "running", null, "周报生成中...");

        try {
            WeeklyDigest weeklyDigest = weeklyDigestService.generateCurrentWeekDigest();
            updateTask(taskId, "success", weeklyDigest.getId(), "周报生成成功");
        } catch (Exception e) {
            updateTask(taskId, "failed", null, e.getClass().getSimpleName() + ": 周报生成失败");
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * 查询任务状态。
     *
     * @param taskId 任务 ID
     * @return 任务状态
     */
    public DigestTaskStatusResponse getTaskStatus(String taskId) {
        DigestTaskStatusResponse response = taskStore.get(taskId);
        if (response == null) {
            throw new IllegalArgumentException("周报任务不存在: " + taskId);
        }

        return response;
    }

    /**
     * 更新任务状态。
     *
     * @param taskId 任务 ID
     * @param status 最新状态
     * @param digestId 周报 ID
     * @param message 状态说明
     */
    private void updateTask(String taskId, String status, Long digestId, String message) {
        DigestTaskStatusResponse oldResponse = taskStore.get(taskId);
        LocalDateTime createdAt = oldResponse == null ? LocalDateTime.now() : oldResponse.createdAt();

        taskStore.put(
                taskId,
                new DigestTaskStatusResponse(
                        taskId,
                        status,
                        digestId,
                        message,
                        createdAt,
                        LocalDateTime.now()
                )
        );
    }

}
