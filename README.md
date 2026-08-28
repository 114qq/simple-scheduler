# simple-scheduler

**A lightweight Java Cron scheduler based on simple-cron and DelayQueue.**  
**轻量级 Java Cron 任务调度器，基于 simple-cron 和 DelayQueue 实现。**

[![Java](https://img.shields.io/badge/Java-8%2B-orange.svg)](https://www.oracle.com/java/)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.114qq/simple-scheduler.svg)](https://central.sonatype.com/)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

---

## 📖 Introduction / 项目介绍

`simple-scheduler` is a small and lightweight Cron task scheduler for Java.

`simple-scheduler` 是一个面向 Java 的轻量级 Cron 任务调度器。

The project is intentionally designed without Spring Boot or other large scheduling frameworks. It uses Java's standard concurrency utilities together with the [`simple-cron`](https://github.com/114qq/simple-cron) library.

本项目有意保持简单，不依赖 Spring Boot 或其他大型任务调度框架，主要使用 JDK 自带的并发工具以及 `simple-cron` 实现。

The core scheduling model is:

```text
Cron Expression
       ↓
CronExpression.next()
       ↓
Next Execution Time
       ↓
DelayQueue
       ↓
Scheduler Thread
       ↓
Worker Thread Pool
       ↓
Runnable
```

---

## ✨ Features / 特性

- Java 8+
- No Spring Boot required / 不依赖 Spring Boot
- Cron-based scheduling / 基于 Cron 的任务调度
- Uses `DelayQueue` / 使用 JDK `DelayQueue`
- Dedicated scheduler thread / 独立调度线程
- Worker thread pool / 工作线程池
- Task registration / 任务注册
- Task cancellation / 任务取消
- Configurable worker pool size / 可配置工作线程数量
- Configurable `ZoneId` / 支持指定时区
- In-memory scheduling / 纯内存调度
- Small dependency footprint / 依赖简单

---

## 📦 Maven

```xml
<dependency>
    <groupId>io.github.114qq</groupId>
    <artifactId>simple-scheduler</artifactId>
    <version>1.0.0</version>
</dependency>
```

`simple-scheduler` depends on:

```xml
<dependency>
    <groupId>io.github.114qq</groupId>
    <artifactId>simple-cron</artifactId>
    <version>1.0.0</version>
</dependency>
```

### GitHub Releases

也可以直接从 [GitHub Releases](https://github.com/114qq/simple-scheduler/releases) 下载 JAR。

当前版本：**1.0.0**

> 推荐优先使用 Maven，Maven 会自动处理 `simple-scheduler` 的传递依赖。
> 如果直接下载 JAR，则需要自行管理运行时依赖。

---

## 🚀 Quick Start / 快速开始

### 1. Create Scheduler / 创建调度器

```java
import com.example.simplescheduler.SimpleScheduler;

public class Main {

    public static void main(String[] args) {

        SimpleScheduler scheduler =
                new SimpleScheduler(4);

        // ...
    }
}
```

The parameter `4` means the scheduler uses a worker pool with four worker threads.

参数 `4` 表示使用 4 个工作线程执行任务。

---

### 2. Schedule a Cron Task / 注册 Cron 任务

```java
scheduler.schedule(
        "heartbeat",
        "* * * * * ?",
        () -> {
            System.out.println(
                    "Heartbeat: " +
                    System.currentTimeMillis()
            );
        }
);
```

The Cron expression:

```text
* * * * * ?
```

means the task runs every second in the `simple-cron` implementation.

该 Cron 表达式表示每秒执行一次。

---

### 3. Cancel a Task / 取消任务

```java
scheduler.cancel("heartbeat");
```

The task is removed from both the task registry and the `DelayQueue`.

取消任务后，任务会从任务注册中心和 `DelayQueue` 中移除。

---

### 4. List Tasks / 查看任务

```java
System.out.println(
        scheduler.taskIds()
);
```

Example:

```text
[heartbeat, backup, cleanup]
```

---

### 5. Shutdown / 关闭调度器

```java
scheduler.shutdown();
```

The scheduler thread is stopped and the worker pool is shut down.

调度线程会停止，工作线程池也会关闭。

---

## 🕐 Time Zone / 时区

By default, the scheduler uses the system default time zone:

```java
SimpleScheduler scheduler =
        new SimpleScheduler(4);
```

You can also specify a time zone:

```java
SimpleScheduler scheduler =
        new SimpleScheduler(
                4,
                ZoneId.of("Asia/Shanghai")
        );
```

For example:

```java
import java.time.ZoneId;

SimpleScheduler scheduler =
        new SimpleScheduler(
                4,
                ZoneId.of("Asia/Shanghai")
        );
```

---

# 🏗️ Architecture / 架构

The first version intentionally uses a small number of components.

第一版刻意保持组件数量较少。

```text
                         SimpleScheduler
                                │
             ┌──────────────────┼──────────────────┐
             │                  │                  │
             ▼                  ▼                  ▼
       TaskRegistry       DelayQueue         TaskExecutor
             │                  │                  │
             │                  ▼                  │
             │          SchedulerThread             │
             │                  │                  │
             │                  │ task.take()      │
             │                  ▼                  │
             │             Task expired             │
             │                  │                  │
             │                  └──────────┐       │
             │                             ▼       ▼
             │                         Worker Pool
             │                             │
             │                             ▼
             │                          Runnable
             │                             │
             │                             ▼
             │                       CronExpression
             │                             │
             │                             ▼
             │                    Next Execution Time
             │                             │
             └─────────────────────────────┴──────→ DelayQueue
```

---

# 🧩 Core Components / 核心组件

## 1. TaskDefinition

`TaskDefinition` describes a user's scheduled task.

`TaskDefinition` 表示用户定义的任务。

```text
TaskDefinition
├── id
├── CronExpression
└── Runnable
```

For example:

```java
TaskDefinition.of(
        "backup",
        "0 0 2 * * ?",
        () -> backup()
);
```

It represents:

```text
Task ID: backup
Cron:    02:00 every day
Action:  backup()
```

---

## 2. TaskRegistry

`TaskRegistry` maintains all registered tasks in memory.

`TaskRegistry` 负责在内存中维护已经注册的任务。

Conceptually:

```text
TaskRegistry
│
├── task-001
├── task-002
├── task-003
└── task-004
```

The first version uses an in-memory `Map`.

第一版使用内存 `Map` 保存任务。

---

## 3. ScheduledTask

`ScheduledTask` is the runtime representation of a scheduled task.

它是任务进入调度系统后的运行时对象。

The important property is:

```text
nextExecutionTime
```

For example:

```text
backup
    ↓
nextExecutionTime
    ↓
2026-08-29 02:00:00
```

`ScheduledTask` implements Java's:

```java
Delayed
```

so it can be stored directly in:

```java
DelayQueue<ScheduledTask>
```

---

## 4. DelayQueue

`DelayQueue` is the core timing mechanism.

`DelayQueue` 是整个调度器的核心时间管理组件。

Suppose there are three tasks:

```text
Task A → 10:01
Task B → 10:05
Task C → 10:02
```

The queue conceptually keeps the earliest task at the head:

```text
Task A
  ↓
Task C
  ↓
Task B
```

The scheduler does not need to continuously scan all tasks.

调度器不需要每秒扫描所有任务。

Instead:

```java
ScheduledTask task = queue.take();
```

`take()` waits until the earliest task becomes available.

当最近的任务到期以后，`take()` 才会返回任务。

This keeps the scheduler simple and avoids unnecessary polling.

---

## 5. SchedulerThread

`SchedulerThread` is responsible for scheduling, not business execution.

`SchedulerThread` 只负责调度，不直接执行用户业务代码。

Its core logic is conceptually:

```java
while (running) {

    ScheduledTask task =
            queue.take();

    executor.execute(task);

    LocalDateTime next =
            task.getDefinition()
                .getCron()
                .next(now);

    task.setNextExecutionTime(next);

    queue.offer(task);
}
```

The important idea is:

```text
Wait
 ↓
Task expires
 ↓
Submit task
 ↓
Calculate next execution time
 ↓
Put task back into DelayQueue
 ↓
Wait again
```

---

## 6. TaskExecutor

The scheduler thread should not execute business code directly.

调度线程不能直接执行用户业务代码。

For example, this design is intentionally avoided:

```java
while (running) {
    ScheduledTask task = queue.take();

    task.getDefinition()
        .getAction()
        .run();
}
```

If the task takes 30 seconds, the scheduler thread would be blocked for 30 seconds.

如果任务执行需要 30 秒，调度线程就会被阻塞 30 秒。

Instead:

```text
SchedulerThread
      ↓
TaskExecutor
      ↓
Worker Pool
      ↓
Runnable
```

This allows the scheduler thread to continue scheduling other tasks.

这样调度线程可以继续处理其他任务。

---

# 🔄 Execution Flow / 执行流程

Suppose we register:

```java
scheduler.schedule(
        "heartbeat",
        "* * * * * ?",
        () -> {
            System.out.println("heartbeat");
        }
);
```

The execution flow is:

```text
1. Parse Cron
      ↓
2. Calculate next execution time
      ↓
3. Create ScheduledTask
      ↓
4. Put ScheduledTask into DelayQueue
      ↓
5. SchedulerThread calls queue.take()
      ↓
6. Wait until task expires
      ↓
7. SchedulerThread receives task
      ↓
8. Submit Runnable to Worker Pool
      ↓
9. Calculate next Cron execution time
      ↓
10. Put task back into DelayQueue
      ↓
11. Repeat
```

So a recurring Cron task is essentially:

```text
      ┌─────────────────────────────┐
      │                             │
      ▼                             │
DelayQueue                          │
      │                             │
      ▼                             │
SchedulerThread                     │
      │                             │
      ▼                             │
Worker Pool                         │
      │                             │
      ▼                             │
Runnable                            │
      │                             │
      ▼                             │
CronExpression.next()               │
      │                             │
      ▼                             │
Next Execution Time ────────────────┘
```

---

# 🧵 Thread Model / 线程模型

The first version uses two levels of execution:

```text
                  SimpleScheduler
                        │
             ┌──────────┴──────────┐
             │                     │
             ▼                     ▼
      SchedulerThread          Worker Pool
                                  │
                         ┌────────┼────────┐
                         ▼        ▼        ▼
                       Worker   Worker   Worker
```

### SchedulerThread

Responsible for:

- Waiting for tasks
- Taking expired tasks
- Calculating the next execution time
- Submitting tasks to the worker pool

### Worker Threads

Responsible for:

- Executing user `Runnable`
- Running business logic
- Isolating slow tasks from the scheduler thread

---

# 🧪 Testing / 测试

The project includes tests for:

- Task registration
- Task cancellation
- Duplicate task ID
- Real task execution
- Multiple scheduled tasks
- Worker thread execution
- Task exceptions

Run:

```bash
mvn clean test
```

For example, the real execution test uses:

```text
* * * * * ?
```

and waits for the scheduler to actually execute the task.

测试并不是单纯测试对象创建，而是包含真实线程调度和任务执行。

---

# 📚 Relation to simple-cron / 与 simple-cron 的关系

The two projects have different responsibilities.

两个项目职责不同。

## simple-cron

Answers:

> **When should the task run?**

回答：

> **任务什么时候执行？**

```text
Cron Expression
      ↓
CronExpression.next()
      ↓
Next DateTime
```

## simple-scheduler

Answers:

> **How should the task be scheduled and executed?**

回答：

> **如何等待并执行这个任务？**

```text
Next DateTime
      ↓
DelayQueue
      ↓
SchedulerThread
      ↓
Worker Pool
      ↓
Runnable
```

Therefore:

```text
simple-cron
     │
     │ calculate
     ▼
Next Execution Time
     │
     ▼
simple-scheduler
     │
     ├── DelayQueue
     ├── SchedulerThread
     ├── TaskRegistry
     └── Worker Pool
```

---

# ⚠️ Limitations / 当前限制

Version `1.0.0` intentionally keeps the implementation small.

`1.0.0` 版本有意保持简单。

Currently it does not provide:

- Database persistence / 数据库持久化
- Redis persistence / Redis 持久化
- Distributed scheduling / 分布式调度
- Distributed locks / 分布式锁
- Cluster coordination / 集群协调
- Retry policies / 重试策略
- Misfire policies / 错过执行策略
- Task history / 任务执行历史
- Web UI / Web 管理界面
- Dynamic configuration center / 动态配置中心
- Spring Boot auto-configuration / Spring Boot 自动配置

This is intentional. The project focuses on understanding and providing a small in-memory scheduler.

这是有意的：项目首先关注一个简单、清晰、容易理解的内存调度器。

---

# 🗺️ Roadmap / 后续计划

Possible future features:

- [ ] Task execution listeners / 任务执行监听器
- [ ] Task execution result / 任务执行结果
- [ ] Retry policy / 重试策略
- [ ] Task status / 任务状态
- [ ] Pause and resume / 暂停与恢复
- [ ] Persistent task store / 持久化任务存储
- [ ] Misfire handling / 错过执行处理
- [ ] Optional Spring Boot integration / 可选 Spring Boot 集成
- [ ] More scheduling policies / 更多调度策略

The project will continue to favor a small and understandable core.

项目会继续保持核心代码简单、清晰、容易理解。

---

# 🛠️ Build from Source / 从源码构建

Clone the repository:

```bash
git clone git@github.com:114qq/simple-scheduler.git
cd simple-scheduler
```

Build:

```bash
mvn clean package
```

Run tests:

```bash
mvn clean test
```

Install to local Maven repository:

```bash
mvn clean install
```

---

# 📄 License / 开源协议

This project is released under the MIT License.

本项目采用 MIT License 开源协议。

See [LICENSE](LICENSE).

---

# 👤 Author

**114qq**

GitHub:

https://github.com/114qq

---

## ⭐ Project Philosophy / 项目理念

`simple-scheduler` is not intended to replace mature distributed scheduling systems.

`simple-scheduler` 并不是为了替代成熟的分布式任务调度系统。

The goal is to provide a small, understandable implementation that demonstrates how a Cron scheduler can be built from basic Java concurrency primitives.

项目的目标是用尽可能少的代码，展示一个 Cron 调度器是如何基于 Java 基础并发工具构建出来的。

Core idea:

```text
Cron
 ↓
Calculate next time
 ↓
DelayQueue
 ↓
Scheduler Thread
 ↓
Worker Pool
 ↓
Execute
 ↓
Calculate next time
 ↓
Repeat
```

**Small core, clear design, easy to understand.**

**核心简单、设计清晰、容易理解。**
