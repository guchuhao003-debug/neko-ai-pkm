package com.wenxi.nekoaipkm.service;

import com.wenxi.nekoaipkm.model.dto.DigestTaskStatusResponse;
import com.wenxi.nekoaipkm.model.entity.WeeklyDigest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;


/**
 * 周报异步任务服务测试
 *
 */
@SpringBootTest(classes = {
        WeeklyDigestTaskService.class,
        WeeklyDigestTaskServiceTest.class
})
class WeeklyDigestTaskServiceTest {

    @Autowired
    private WeeklyDigestTaskService weeklyDigestTaskService;

    @MockitoBean
    private WeeklyDigestService weeklyDigestService;

    /**
     * 测试创建任务后查看 pending 任务状态
     */
    @Test
    void createPendingTask() {
        String taskId = weeklyDigestTaskService.createTask();
        DigestTaskStatusResponse response = weeklyDigestTaskService.getTaskStatus(taskId);
        assertNotNull(response);
        assertEquals(taskId, response.taskId());
        assertEquals("pending", response.status());
        assertEquals("周报任务已创建", response.message());

    }

    /**
     * 周报生成成功后任务状态应该变为 success
     */
    @Test
    void updateTaskStatusWhenDigestGenerated() {
        WeeklyDigest weeklyDigest = new WeeklyDigest();
        weeklyDigest.setId(1001L);

        when(weeklyDigestService.generateCurrentWeekDigest()).thenReturn(weeklyDigest);

        String taskId = weeklyDigestTaskService.createTask();
        CompletableFuture<Void> future = weeklyDigestTaskService.generateCurrentWeekDigestAsync(taskId);

        DigestTaskStatusResponse response = weeklyDigestTaskService.getTaskStatus(taskId);

        assertEquals("success",response.status());
        assertEquals(1001L,response.digestId());
        assertEquals("周报生成完成",response.message());
    }

    /**
     * 周报生成失败后任务状态应该变为 failed
     */
    @Test
    void updateTaskStatusWhenDigestFailed() {
        when(weeklyDigestService.generateCurrentWeekDigest())
                .thenThrow(new IllegalStateException("mock error"));

        String taskId = weeklyDigestTaskService.createTask();
        CompletableFuture<Void> future = weeklyDigestTaskService.generateCurrentWeekDigestAsync(taskId);
        future.join();

        DigestTaskStatusResponse response = weeklyDigestTaskService.getTaskStatus(taskId);

        assertEquals("failed", response.status());
        assertTrue(response.message().contains("IllegalStateException"));
        assertTrue(response.message().contains("周报生成失败"));
    }

    /**
     * 测试环境异步线程池配置
     */
    @TestConfiguration
    @EnableAsync
    static class TestAsyncConfig {

        /**
         * 周报任务测试线程池
         * @return  测试任务执行器
         */
        @Bean("digestTaskExecutor")
        public Executor digestTaskExecutor() {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(1);
            executor.setMaxPoolSize(1);
            executor.setQueueCapacity(10);
            executor.setThreadNamePrefix("test-digest-task-");
            executor.initialize();
            return executor;
        }
    }


}
