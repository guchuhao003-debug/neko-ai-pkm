package com.wenxi.nekoaipkm.service;


import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 知识库定时任务，负责周期性生成知识周报
 */
@Component
@RequiredArgsConstructor
public class KnowledgeScheduleTask {

    private final  WeeklyDigestTaskService weeklyDigestTaskService;

    @Scheduled(cron = "0 0 9 * * MON", zone = "Asia/Shanghai")
    public void generateWeeklyDigest() {
        String taskId = weeklyDigestTaskService.createTask();
        weeklyDigestTaskService.generateCurrentWeekDigestAsync(taskId);
    }
}
