package com.example.simplescheduler;

import java.util.List;

/**
 * Public scheduler API.
 */
public interface Scheduler {
    void start();
    void schedule(String id, String cronExpression, Runnable action);
    void cancel(String id);
    boolean isRunning();
    List<String> taskIds();
    void shutdown();
}
