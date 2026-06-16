package com.wenxi.nekoaipkm.config;

import com.wenxi.nekoaipkm.service.WeeklyDigestService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 知识库定时任务，负责周期性生成知识周报
 */
@Component
@RequiredArgsConstructor
public class KnowledgeScheduleTask {

    private final WeeklyDigestService weeklyDigestService;

    /**
     * 每周一上午 9 点生成上一周期的知识回顾。
     */
    @Scheduled(cron = "0 0 9 * * MON")
    public void generateWeeklyDigest() {
        weeklyDigestService.generateCurrentWeekDigest();
    }
}
