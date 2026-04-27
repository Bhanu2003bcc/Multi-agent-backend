package com.research.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@Slf4j
public class AsyncConfig implements AsyncConfigurer {

    /**
     * Dedicated thread pool for executing research pipeline jobs.
     * Sized conservatively — each job holds a thread for up to 2 minutes.
     */
    @Bean("researchTaskExecutor")
    public AsyncTaskExecutor researchTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(32);
        executor.setQueueCapacity(500);
        executor.setKeepAliveSeconds(120);
        executor.setThreadNamePrefix("research-pipeline-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120);
        executor.setRejectedExecutionHandler((runnable, pool) ->
                log.error("Research task rejected — queue full! Increase pool size.")
        );
        executor.initialize();
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return researchTaskExecutor();
    }

    @Override
    public org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler
            getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) ->
                log.error("Uncaught async exception in method=[{}]: {}",
                        method.getName(), ex.getMessage(), ex);
    }
}
