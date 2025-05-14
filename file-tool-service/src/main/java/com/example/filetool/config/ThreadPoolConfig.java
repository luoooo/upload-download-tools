package com.example.filetool.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池配置类
 * 用于处理文件上传下载的并发任务
 */
@Configuration
@EnableAsync
public class ThreadPoolConfig {

    /**
     * 核心线程数
     */
    @Value("${file.task.thread-pool.core-size:5}")
    private int corePoolSize;

    /**
     * 最大线程数
     */
    @Value("${file.task.thread-pool.max-size:10}")
    private int maxPoolSize;

    /**
     * 队列容量
     */
    @Value("${file.task.thread-pool.queue-capacity:100}")
    private int queueCapacity;

    /**
     * 线程空闲时间（秒）
     */
    @Value("${file.task.thread-pool.keep-alive-seconds:60}")
    private int keepAliveSeconds;

    /**
     * 文件处理线程池
     */
    @Bean("fileTaskExecutor")
    public Executor fileTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setThreadNamePrefix("file-task-");
        
        // 设置拒绝策略：当池满时，调用者线程执行任务
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 等待所有任务结束后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        
        // 等待时间（秒）
        executor.setAwaitTerminationSeconds(60);
        
        return executor;
    }
}