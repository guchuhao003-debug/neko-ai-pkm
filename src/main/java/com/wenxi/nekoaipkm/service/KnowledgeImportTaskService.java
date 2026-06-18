package com.wenxi.nekoaipkm.service;

import cn.hutool.core.util.IdUtil;
import com.wenxi.nekoaipkm.model.vo.ImportResponse;
import com.wenxi.nekoaipkm.model.vo.ImportTaskStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;


/**
 * 知识导入任务服务，用于管理后台导入任务状态。
 *
 */
@Service
@RequiredArgsConstructor
public class KnowledgeImportTaskService {

    private final KnowledgeImportService knowledgeImportService;

    private final Map<String, ImportTaskStatusResponse> taskStatusMap = new ConcurrentHashMap<>();

    /**
     * 创建导入任务 ID
     *
     * @return  任务 ID
     */
    public String createTask() {
        String taskId = IdUtil.fastSimpleUUID();
        taskStatusMap.put(taskId, ImportTaskStatusResponse.running(taskId));
        return taskId;
    }

    /**
     * 查询任务状态。
     *
     * @param taskId 任务 ID
     * @return 任务状态
     */
    public ImportTaskStatusResponse getTaskStatus(String taskId) {
        ImportTaskStatusResponse status = taskStatusMap.get(taskId);
        if (status == null) {
            throw new IllegalStateException("导入任务不存在: " + taskId);
        }
        return status;
    }

    /**
     * 异步扫描本地目录并导入。
     *
     * @param taskId 任务 ID
     * @return 异步任务
     */
    @Async
    public CompletableFuture<Void> scanAndImportAsync(String taskId) {
        runTask(taskId, knowledgeImportService::scanAndImport);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 异步导入上传的 Markdown 文件。
     *
     * @param taskId 任务 ID
     * @param markdownFile 上传文件路径
     * @param sourcePath COS 来源路径
     * @return 异步任务
     */
    @Async
    public CompletableFuture<Void> importUploadedMarkdownAsync(String taskId, Path markdownFile, String sourcePath) {
        runTask(taskId, () -> {
            boolean imported = knowledgeImportService.importUploadedMarkdown(markdownFile, sourcePath);
            return new ImportResponse(1,imported ? 1 : 0,imported ? 0 : 1);
        });
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 执行导入任务并记录状态。
     *
     * @param taskId 任务 ID
     * @param action 导入动作
     */
    private void runTask(String taskId, Supplier<ImportResponse> action) {
        taskStatusMap.put(taskId, ImportTaskStatusResponse.running(taskId));

        try {
            ImportResponse result = action.get();
            taskStatusMap.put(taskId, ImportTaskStatusResponse.success(taskId, result));
        } catch (Exception e) {
            String safeErrorMessage = buildSafeErrorMessage(e);
            taskStatusMap.put(taskId, ImportTaskStatusResponse.failed(taskId,safeErrorMessage));
        }
    }

    /**
     * 文档错误信息脱敏处理
     * @param e
     * @return
     */
    private String buildSafeErrorMessage(Exception e) {
        String message = e.getMessage();
        if (message != null && message.contains("input texts limit")) {
            return "文档分块数量过多，请降低向量化批量大小后重试";
        }
        return "文档导入失败，请查看后台日志";
    }
}
