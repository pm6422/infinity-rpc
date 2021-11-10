package org.infinity.luix.democlient.config;

import lombok.extern.slf4j.Slf4j;
import org.infinity.luix.democlient.async.ExceptionHandlingAsyncTaskExecutor;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.boot.autoconfigure.task.TaskExecutionProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.annotation.Resource;
import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@EnableScheduling
@Slf4j
public class AsyncConfiguration implements AsyncConfigurer {

    @Resource
    private TaskExecutionProperties taskExecutionProperties;

    @Override
    @Bean(name = "asyncTaskExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(taskExecutionProperties.getPool().getCoreSize());
        executor.setMaxPoolSize(taskExecutionProperties.getPool().getMaxSize());
        executor.setQueueCapacity(taskExecutionProperties.getPool().getQueueCapacity());
        executor.setThreadNamePrefix(taskExecutionProperties.getThreadNamePrefix());
        log.info("Created async task executor with corePoolSize: [{}], maxPoolSize: [{}] and queueCapacity: [{}]",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), taskExecutionProperties.getPool().getQueueCapacity());
        return new ExceptionHandlingAsyncTaskExecutor(executor);
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new SimpleAsyncUncaughtExceptionHandler();
    }

//    @Bean
//    public ThreadPoolTaskScheduler taskScheduler() {
//        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
//        taskScheduler.setPoolSize(10);
//        taskScheduler.setRemoveOnCancelPolicy(true);
//        taskScheduler.setErrorHandler(t -> log.error("Unexpected error occurred in scheduled task.", t));
//        taskScheduler.setBeanName(applicationProperties.getCache().getCachePrefix() + "thread");
//        return taskScheduler;
//    }
}
