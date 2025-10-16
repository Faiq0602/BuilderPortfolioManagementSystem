package com.builder.portfolio.util;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides fine grained read/write locks per project id.
 */
public final class LockRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(LockRegistry.class);
    private static final LockRegistry INSTANCE = new LockRegistry();

    private final ConcurrentHashMap<Long, ReentrantReadWriteLock> projectLocks = new ConcurrentHashMap<>();

    private LockRegistry() {
    }

    public static LockRegistry getInstance() {
        return INSTANCE;
    }

    public ReentrantReadWriteLock getProjectLock(long projectId) {
        return projectLocks.computeIfAbsent(projectId, id -> new ReentrantReadWriteLock(true));
    }

    public <T> T withProjectRead(long projectId, Supplier<T> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        ReentrantReadWriteLock lock = getProjectLock(projectId);
        return withLock(lock.readLock(), projectId, "read", supplier);
    }

    public void withProjectRead(long projectId, Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable");
        withProjectRead(projectId, () -> {
            runnable.run();
            return null;
        });
    }

    public <T> T withProjectWrite(long projectId, Supplier<T> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        ReentrantReadWriteLock lock = getProjectLock(projectId);
        return withLock(lock.writeLock(), projectId, "write", supplier);
    }

    public void withProjectWrite(long projectId, Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable");
        withProjectWrite(projectId, () -> {
            runnable.run();
            return null;
        });
    }

    private <T> T withLock(Lock lock, long projectId, String mode, Supplier<T> supplier) {
        long start = System.nanoTime();
        String threadName = Thread.currentThread().getName();
        // These debug traces make it painless to spot contention while running the console demo.
        LOGGER.debug("Thread {} attempting {} lock for project {}", threadName, mode, projectId);
        lock.lock();
        try {
            LOGGER.debug("Thread {} acquired {} lock for project {} in {} µs", threadName, mode,
                    projectId, TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - start));
            return supplier.get();
        } finally {
            lock.unlock();
            LOGGER.debug("Thread {} released {} lock for project {}", threadName, mode, projectId);
        }
    }
}
