package com.example.simplescheduler;

import com.example.simplecron.CronExpression;

import java.util.Objects;

/**
 * Immutable definition of a scheduled Cron task.
 */
public final class TaskDefinition {
    private final String id;
    private final CronExpression cron;
    private final Runnable action;

    private TaskDefinition(String id, CronExpression cron, Runnable action) {
        this.id = id;
        this.cron = cron;
        this.action = action;
    }

    public static TaskDefinition of(String id, String expression, Runnable action) {
        if (id == null || id.trim().isEmpty()) {
            throw new SchedulerException("Task id is empty");
        }
        if (action == null) {
            throw new SchedulerException("Task action is null");
        }
        return new TaskDefinition(id, CronExpression.parse(expression), action);
    }

    public String getId() { return id; }
    public CronExpression getCron() { return cron; }
    public Runnable getAction() { return action; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskDefinition)) return false;
        TaskDefinition that = (TaskDefinition) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
