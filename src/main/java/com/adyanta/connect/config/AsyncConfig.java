package com.adyanta.connect.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for asynchronous processing
 * 
 * This configuration sets up thread pools for @Async methods
 * to handle background processing tasks.
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Task executor for general async operations
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("ConnectService-Async-");
        executor.setRejectedExecutionHandler((r, executor1) -> {
            log.warn("Task rejected from async executor. Queue might be full. Task: {}", r.toString());
            // Fallback to calling runnable in current thread
            r.run();
        });
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * Task executor for processing requests
     */
    @Bean(name = "processingExecutor")
    public Executor processingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("ConnectService-Processing-");
        executor.setRejectedExecutionHandler((r, executor1) -> {
            log.error("Processing task rejected. This should not happen. Task: {}", r.toString());
            throw new RuntimeException("Processing task rejected - system overloaded");
        });
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120);
        executor.initialize();
        return executor;
    }

    /**
     * Task executor for external API calls
     */
    @Bean(name = "externalApiExecutor")
    public Executor externalApiExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(15);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("ConnectService-ExternalAPI-");
        executor.setRejectedExecutionHandler((r, executor1) -> {
            log.warn("External API task rejected. Queue might be full. Task: {}", r.toString());
            // For external API calls, we can retry later
            r.run();
        });
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}