package com.example.simplescheduler;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Single scheduler thread. It waits on DelayQueue and submits expired
 * tasks to the worker pool.
 */
final class SchedulerThread extends Thread {
    private final DelayQueue<ScheduledTask> queue;
    private final TaskExecutor executor;
    private final ZoneId zoneId;
    private final AtomicBoolean running = new AtomicBoolean(true);

    SchedulerThread(
            DelayQueue<ScheduledTask> queue,
            TaskExecutor executor,
            ZoneId zoneId) {
        super("simple-scheduler");
        setDaemon(true);
        this.queue = queue;
        this.executor = executor;
        this.zoneId = zoneId;
    }

    void shutdown() {
        running.set(false);
        interrupt();
    }

    @Override
    public void run() {
        while (running.get()) {
            try {
                ScheduledTask task = queue.take();

                LocalDateTime now = LocalDateTime.now(zoneId);

                executor.execute(task);

                LocalDateTime next = task.getDefinition().getCron().next(now);
                task.setNextExecutionTime(next);

                if (running.get()) {
                    queue.offer(task);
                }
            } catch (InterruptedException e) {
                if (running.get()) {
                    Thread.currentThread().interrupt();
                    break;
                }
            } catch (Throwable e) {
                System.err.println("Scheduler thread error");
                e.printStackTrace();
            }
        }
    }
}
