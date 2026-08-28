package com.example.simplescheduler;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * Runtime task stored in the DelayQueue.
 */
final class ScheduledTask implements Delayed {
    private final TaskDefinition definition;
    private volatile LocalDateTime nextExecutionTime;
    private final ZoneId zoneId;

    ScheduledTask(TaskDefinition definition, LocalDateTime nextExecutionTime, ZoneId zoneId) {
        this.definition = definition;
        this.nextExecutionTime = nextExecutionTime;
        this.zoneId = zoneId;
    }

    TaskDefinition getDefinition() { return definition; }
    LocalDateTime getNextExecutionTime() { return nextExecutionTime; }

    void setNextExecutionTime(LocalDateTime nextExecutionTime) {
        this.nextExecutionTime = nextExecutionTime;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        long millis = nextExecutionTime.atZone(zoneId).toInstant().toEpochMilli()
                - System.currentTimeMillis();
        return unit.convert(millis, TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed other) {
        return Long.compare(
                getDelay(TimeUnit.NANOSECONDS),
                other.getDelay(TimeUnit.NANOSECONDS)
        );
    }
}
