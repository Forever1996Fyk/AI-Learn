package com.forever1996Fyk.ai.intelligent.customer.document.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/4 17:25
 **/
@Slf4j
@EnableAsync
@Configuration
@EnableScheduling
public class AsyncConfiguration {

    /**
     * 事件监听专用线程池
     * 用于处理文档状态变更事件的异步监听
     */
    @Bean("documentEventListenerExecutor")
    public Executor documentEventListenerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数
        executor.setCorePoolSize(4);

        // 最大线程数
        executor.setMaxPoolSize(8);

        // 队列容量
        executor.setQueueCapacity(50);

        // 线程名前缀
        executor.setThreadNamePrefix("document-event-listener-");

        // 线程空闲时间
        executor.setKeepAliveSeconds(60);

        // 拒绝策略
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 优雅关闭
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();

        log.info("事件监听线程池初始化完成");

        return executor;
    }
}
