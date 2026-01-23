package com.techno.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for asynchronous task execution.
 *
 * Enables async processing for:
 * - NotificationEventListener: Processes notification events
 * - EmailService: Sends emails without blocking business logic
 *
 * Thread pool configuration:
 * - Core pool size: 5 threads (always active)
 * - Max pool size: 10 threads (during high load)
 * - Queue capacity: 100 tasks (buffering)
 * - Thread name prefix: "notification-async-"
 *
 * Benefits:
 * - Notifications don't block business operations
 * - Better user experience (faster response times)
 * - Isolation of notification failures
 * - Scalable under load
 *
 * Error handling:
 * - Uncaught exceptions are logged
 * - Failures don't affect business logic
 * - Can be monitored and alerted
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 9 - Notifications & Email System
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    /**
     * Thread pool executor for async notification processing.
     *
     * Configuration:
     * - corePoolSize: 5 threads always active
     * - maxPoolSize: 10 threads maximum
     * - queueCapacity: 100 tasks can be queued
     * - threadNamePrefix: "notification-async-" for easy identification
     * - waitForTasksToCompleteOnShutdown: true (graceful shutdown)
     * - awaitTerminationSeconds: 60 (wait up to 60 seconds for tasks to complete)
     *
     * Pool behavior:
     * 1. Tasks 1-5: Use core threads
     * 2. Tasks 6-105: Queue up to 100 tasks
     * 3. Tasks 106-110: Create new threads (up to max 10)
     * 4. Task 111+: Reject with CallerRunsPolicy (run in caller thread)
     *
     * @return Configured thread pool executor
     */
    @Bean(name = "taskExecutor")
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Core pool: always-active threads
        executor.setCorePoolSize(5);

        // Max pool: maximum threads under high load
        executor.setMaxPoolSize(10);

        // Queue: buffer tasks when all core threads are busy
        executor.setQueueCapacity(100);

        // Thread naming for easy debugging
        executor.setThreadNamePrefix("notification-async-");

        // Graceful shutdown: wait for tasks to complete
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        // Rejection policy: run in caller thread if pool is full
        // This prevents task loss but may slow down the caller
        executor.setRejectedExecutionHandler(
                new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize();

        log.info("Async task executor initialized: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());

        return executor;
    }

    /**
     * Exception handler for uncaught async exceptions.
     *
     * Logs all exceptions that occur in async methods.
     * This ensures errors don't get silently swallowed.
     *
     * @return Exception handler
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) -> {
            log.error("Uncaught exception in async method: method={}, params={}, error={}",
                    method.getName(),
                    params != null ? params.length : 0,
                    throwable.getMessage(),
                    throwable);

            // TODO: Consider sending alert to admin for critical async failures
            // TODO: Consider retry mechanism for certain types of failures
        };
    }

    /**
     * Optional: Email-specific executor with different pool sizes.
     *
     * Use this if email sending requires different resource allocation
     * than general notification processing.
     *
     * To use: @Async("emailExecutor") in EmailService methods
     *
     * @return Email-specific executor
     */
    @Bean(name = "emailExecutor")
    public Executor getEmailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Smaller pool for email sending
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("email-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();

        log.info("Email task executor initialized: corePoolSize={}, maxPoolSize={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize());

        return executor;
    }
}
