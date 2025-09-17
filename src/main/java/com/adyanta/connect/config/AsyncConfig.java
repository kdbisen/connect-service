package com.adyanta.connect.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for async processing
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Value("${processing.async.core-pool-size:10}")
    private int corePoolSize;
    
    @Value("${processing.async.max-pool-size:50}")
    private int maxPoolSize;
    
    @Value("${processing.async.queue-capacity:1000}")
    private int queueCapacity;
    
    @Bean(name = "processingExecutor")
    public Executor processingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("ConnectService-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        
        log.info("Configured async executor: core={}, max={}, queue={}", 
                corePoolSize, maxPoolSize, queueCapacity);
        
        return executor;
    }
}

