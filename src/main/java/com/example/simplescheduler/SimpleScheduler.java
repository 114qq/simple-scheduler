package com.example.simplescheduler;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.DelayQueue;

/**
 * Lightweight in-memory Cron scheduler.
 */
public final class SimpleScheduler implements Scheduler {
    private final ZoneId zoneId;
    private final DelayQueue<ScheduledTask> queue = new DelayQueue<ScheduledTask>();
    private final TaskRegistry registry = new TaskRegistry();
    private final TaskExecutor executor;
    private final Object lifecycleLock = new Object();

    private volatile SchedulerThread schedulerThread;

    public SimpleScheduler() {
        this(Runtime.getRuntime().availableProcessors());
    }

    public SimpleScheduler(int workerPoolSize) {
        this(workerPoolSize, ZoneId.systemDefault());
    }

    public SimpleScheduler(int workerPoolSize, ZoneId zoneId) {
        if (zoneId == null) {
            throw new IllegalArgumentException("zoneId is null");
        }
        this.zoneId = zoneId;
        this.executor = new TaskExecutor(workerPoolSize);
    }

    @Override
    public void start() {
        synchronized (lifecycleLock) {
            if (isRunning()) {
                return;
            }
            SchedulerThread thread =
                    new SchedulerThread(queue, executor, zoneId);
            schedulerThread = thread;
            thread.start();
        }
    }

    @Override
    public void schedule(String id, String cronExpression, Runnable action) {
        TaskDefinition definition =
                TaskDefinition.of(id, cronExpression, action);

        LocalDateTime next =
                definition.getCron().next(LocalDateTime.now(zoneId));

        ScheduledTask task =
                new ScheduledTask(definition, next, zoneId);

        synchronized (lifecycleLock) {
            registry.register(task);
            queue.offer(task);

            if (!isRunning()) {
                start();
            }
        }
    }

    @Override
    public void cancel(String id) {
        synchronized (lifecycleLock) {
            ScheduledTask task = registry.remove(id);
            if (task != null) {
                queue.remove(task);
            }
        }
    }

    @Override
    public boolean isRunning() {
        SchedulerThread thread = schedulerThread;
        return thread != null && thread.isAlive();
    }

    @Override
    public List<String> taskIds() {
        return registry.ids();
    }

    @Override
    public void shutdown() {
        synchronized (lifecycleLock) {
            SchedulerThread thread = schedulerThread;
            if (thread != null) {
                thread.shutdown();
                schedulerThread = null;
            }
            executor.shutdown();
            queue.clear();
        }
    }
}
