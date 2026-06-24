package com.wenxi.nekoaipkm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步任务配置
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    /**
     * 周报生成任务线程池，避免 LLM 调用阻塞 Web 请求线程
     *
     * @return  任务执行器
     */
    @Bean("digestTaskExecutor")
    public Executor digestTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("digest-task-");
        executor.initialize();
        return executor;
    }
}
