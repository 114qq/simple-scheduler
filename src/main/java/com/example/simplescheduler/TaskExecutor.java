package com.example.simplescheduler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Executes business tasks outside the scheduler thread.
 */
final class TaskExecutor {
    private final ExecutorService executor;

    TaskExecutor(int poolSize) {
        if (poolSize <= 0) {
            throw new IllegalArgumentException("poolSize must be greater than 0");
        }

        executor = Executors.newFixedThreadPool(poolSize, new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(
                        r,
                        "simple-scheduler-worker-" + counter.getAndIncrement()
                );
                thread.setDaemon(true);
                return thread;
            }
        });
    }

    void execute(final ScheduledTask task) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    task.getDefinition().getAction().run();
                } catch (Throwable e) {
                    System.err.println(
                            "Scheduled task failed: " + task.getDefinition().getId()
                    );
                    e.printStackTrace();
                }
            }
        });
    }

    void shutdown() {
        executor.shutdown();
    }
}
