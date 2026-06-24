package com.wenxi.nekoaipkm.controller;

import com.wenxi.nekoaipkm.model.dto.DigestTaskResponse;
import com.wenxi.nekoaipkm.model.dto.DigestTaskStatusResponse;
import com.wenxi.nekoaipkm.service.WeeklyDigestTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * 周报生成接口，负责提交周报生成任务和查询任务状态
 */
@RestController
@RequestMapping("/digests")
@RequiredArgsConstructor
public class DigestController {

    private final WeeklyDigestTaskService weeklyDigestTaskService;

    /**
     * 提交当前自然周周报生成任务
     *
     * @return  任务提交结果
     */
    @PostMapping("/weekly/tasks")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public DigestTaskResponse submitWeeklyDigestTask() {
        String taskId = weeklyDigestTaskService.createTask();
        weeklyDigestTaskService.generateCurrentWeekDigestAsync(taskId);

        return new DigestTaskResponse(taskId,"running", "周报生成任务已提交");
    }

    /**
     * 查询周报任务状态
     *
     * @param taskId    任务 ID
     * @return  任务状态
     */
    @GetMapping("/tasks/{taskId}")
    public DigestTaskStatusResponse getDigestTaskStatus(@PathVariable String taskId) {
        return weeklyDigestTaskService.getTaskStatus(taskId);
    }

}
