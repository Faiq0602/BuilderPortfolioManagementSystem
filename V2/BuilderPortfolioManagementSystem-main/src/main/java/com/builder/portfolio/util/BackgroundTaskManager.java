package com.builder.portfolio.util;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Centralizes thread pools for user initiated and scheduled tasks.
 */
public final class BackgroundTaskManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackgroundTaskManager.class);
    private static final BackgroundTaskManager INSTANCE = new BackgroundTaskManager();

    private final ExecutorService userOpsPool;
    private final ScheduledExecutorService scheduledPool;

    private BackgroundTaskManager() {
        this.userOpsPool = Executors.newFixedThreadPool(
                Math.max(4, Runtime.getRuntime().availableProcessors()),
                namedFactory("bpms-user"));
        this.scheduledPool = Executors.newScheduledThreadPool(2, namedFactory("bpms-sched"));
        // Hook into JVM shutdown so long-running demos or schedulers do not leave straggler threads around.
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown, "bpms-task-shutdown"));
    }

    public static BackgroundTaskManager getInstance() {
        return INSTANCE;
    }

    public ExecutorService getUserOpsPool() {
        return userOpsPool;
    }

    public ScheduledExecutorService getScheduledPool() {
        return scheduledPool;
    }

    public <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, userOpsPool);
    }

    public CompletableFuture<Void> runAsync(Runnable runnable) {
        return CompletableFuture.runAsync(runnable, userOpsPool);
    }

    public void shutdown() {
        LOGGER.info("Shutting down background executors");
        userOpsPool.shutdown();
        scheduledPool.shutdown();
        try {
            if (!userOpsPool.awaitTermination(5, TimeUnit.SECONDS)) {
                userOpsPool.shutdownNow();
            }
            if (!scheduledPool.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduledPool.shutdownNow();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            userOpsPool.shutdownNow();
            scheduledPool.shutdownNow();
        }
    }

    public void scheduleWithFixedDelay(Runnable runnable, Duration initialDelay, Duration delay) {
        Objects.requireNonNull(runnable, "runnable");
        scheduledPool.scheduleWithFixedDelay(runnable,
                initialDelay.toMillis(), delay.toMillis(), TimeUnit.MILLISECONDS);
    }

    private ThreadFactory namedFactory(String prefix) {
        AtomicInteger counter = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, prefix + '-' + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }
}
