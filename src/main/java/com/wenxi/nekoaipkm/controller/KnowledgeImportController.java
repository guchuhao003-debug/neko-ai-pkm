package com.wenxi.nekoaipkm.controller;

import com.wenxi.nekoaipkm.model.dto.CosUploadResult;
import com.wenxi.nekoaipkm.model.vo.ImportResponse;
import com.wenxi.nekoaipkm.model.vo.ImportTaskResponse;
import com.wenxi.nekoaipkm.model.vo.ImportTaskStatusResponse;
import com.wenxi.nekoaipkm.service.CosStorageService;
import com.wenxi.nekoaipkm.service.KnowledgeImportService;
import com.wenxi.nekoaipkm.service.KnowledgeImportTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

/**
 * 知识导入接口，负责触发本地笔记扫描
 *
 * @RequiredArgsConstructor 是 Lombok 提供的一个注解，用于 自动生成包含 final 或 @NonNull 修饰字段的构造方法，
 * 从而减少样板代码（boilerplate code），尤其适合 Spring 构造器注入 场景。
 */

@RestController
@RequestMapping("/knowledge")
@RequiredArgsConstructor
public class KnowledgeImportController {

    // 此处通过@RequiredArgsConstructor 注解构造器自动注入
    private final KnowledgeImportService knowledgeImportService;

    private final CosStorageService cosStorageService;

    private final KnowledgeImportTaskService knowledgeImportTaskService;

    /**
     * 同步扫描配置目录下的 Markdown 文件。
     *
     * @return 导入统计结果
     */
    @PostMapping("/import/scan")
    public ImportResponse scanAndImport() {
        return knowledgeImportService.scanAndImport();
    }

    /**
     * 异步扫描配置目录下的 Markdown 文件。
     *
     * @return 任务提交结果
     */
    @PostMapping("/import/scan/tasks")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ImportTaskResponse submitScanTask() {
        String taskId = knowledgeImportTaskService.createTask();
        knowledgeImportTaskService.scanAndImportAsync(taskId);

        return new ImportTaskResponse(taskId, "running", "本地目录扫描导入任务已提交");
    }

    /**
     * 上传 Markdown 文件并提交异步导入任务。
     *
     * @param file 上传文件
     * @return 任务提交结果
     */
    @PostMapping(value = "/import/upload/tasks", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ImportTaskResponse uploadAndImport(@RequestParam("file")MultipartFile file) {
        CosUploadResult uploadResult = cosStorageService.uploadMarkDown(file);
        Path tempFile = knowledgeImportService.saveUploadedMarkdownToTemp(file);
        String taskId = knowledgeImportTaskService.createTask();
        knowledgeImportTaskService.importUploadedMarkdownAsync(taskId, tempFile, uploadResult.cosUri());

        return new ImportTaskResponse(taskId,"running","上传文件导任务已提交");
    }

    /**
     * 查询导入任务状态。
     *
     * @param taskId 任务 ID
     * @return 任务状态
     */
    @GetMapping("/import/tasks/{taskId}")
    public ImportTaskStatusResponse getTaskStatus(@PathVariable String taskId) {
        return knowledgeImportTaskService.getTaskStatus(taskId);
    }
}
