package com.example.simplescheduler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory task registry.
 */
public final class TaskRegistry {
    private final Map<String, ScheduledTask> tasks = new LinkedHashMap<String, ScheduledTask>();

    public synchronized void register(ScheduledTask task) {
        String id = task.getDefinition().getId();
        if (tasks.containsKey(id)) {
            throw new SchedulerException("Task already exists: " + id);
        }
        tasks.put(id, task);
    }

    public synchronized ScheduledTask remove(String id) {
        return tasks.remove(id);
    }

    public synchronized List<String> ids() {
        return Collections.unmodifiableList(new ArrayList<String>(tasks.keySet()));
    }
}
