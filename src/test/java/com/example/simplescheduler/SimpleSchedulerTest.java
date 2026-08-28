package com.example.simplescheduler;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class SimpleSchedulerTest {

    /**
     * 测试任务注册和取消。
     */
    @Test
    void shouldRegisterAndCancelTask() {
        SimpleScheduler scheduler = new SimpleScheduler(1);

        try {
            scheduler.schedule(
                    "task-1",
                    "0 */5 * * * ?",
                    new Runnable() {
                        @Override
                        public void run() {
                            System.out.println("task-1 executed");
                        }
                    }
            );

            List<String> ids = scheduler.taskIds();

            assertTrue(ids.contains("task-1"));

            scheduler.cancel("task-1");

            assertFalse(scheduler.taskIds().contains("task-1"));

        } finally {
            scheduler.shutdown();
        }
    }

    /**
     * 测试重复任务 ID。
     */
    @Test
    void shouldRejectDuplicateTaskId() {
        SimpleScheduler scheduler = new SimpleScheduler(1);

        try {
            scheduler.schedule(
                    "same-id",
                    "0 */2 * * * ?",
                    new Runnable() {
                        @Override
                        public void run() {
                            System.out.println("same-id executed */2");
                        }
                    }
            );

            assertThrows(
                    SchedulerException.class,
                    () -> scheduler.schedule(
                            "same-id",
                            "0 */10 * * * ?",
                            new Runnable() {
                                @Override
                                public void run() {
                                    System.out.println("same-id executed */10");
                                }
                            }
                    )
            );

        } finally {
            scheduler.shutdown();
        }
    }

    /**
     * 测试任务真正被线程执行。
     *
     * Cron:
     *
     *     * * * * * ?
     *
     * 表示每秒执行一次。
     */
    @Test
    void shouldExecuteTask() throws Exception {

        final CountDownLatch latch =
                new CountDownLatch(1);

        SimpleScheduler scheduler =
                new SimpleScheduler(2);

        try {

            scheduler.schedule(
                    "execute-test",
                    "* * * * * ?",
                    new Runnable() {
                        @Override
                        public void run() {

                            System.out.println(
                                    "[execute-test] "
                                            + LocalDateTime.now()
                                            + " thread="
                                            + Thread.currentThread().getName()
                            );

                            latch.countDown();
                        }
                    }
            );

            assertTrue(
                    latch.await(70, TimeUnit.SECONDS)
            );

        } finally {
            scheduler.shutdown();
        }
    }

    /**
     * 真正观察 Cron 任务连续执行。
     *
     * 每秒打印一次：
     *
     *     *
     *     *
     *     *
     *
     * 可以直观看到 SchedulerThread
     * 和 Worker Thread 在工作。
     */
    @Test
    void shouldExecuteEverySecond() throws Exception {

        final CountDownLatch latch =
                new CountDownLatch(5);

        final AtomicInteger counter =
                new AtomicInteger(0);

        SimpleScheduler scheduler =
                new SimpleScheduler(2);

        try {

            scheduler.schedule(
                    "heartbeat",
                    "* * * * * ?",
                    new Runnable() {
                        @Override
                        public void run() {

                            int count =
                                    counter.incrementAndGet();

                            System.out.println(
                                    "***** HEARTBEAT #"
                                            + count
                                            + " | time="
                                            + LocalDateTime.now()
                                            + " | thread="
                                            + Thread.currentThread().getName()
                            );

                            latch.countDown();
                        }
                    }
            );

            assertTrue(
                    latch.await(10, TimeUnit.SECONDS)
            );

            assertTrue(
                    counter.get() >= 5
            );

        } finally {
            scheduler.shutdown();
        }
    }

    /**
     * 测试多个任务同时运行。
     */
    @Test
    void shouldExecuteMultipleTasks() throws Exception {

        final CountDownLatch latch =
                new CountDownLatch(6);

        SimpleScheduler scheduler =
                new SimpleScheduler(3);

        try {

            scheduler.schedule(
                    "task-A",
                    "* * * * * ?",
                    new Runnable() {
                        @Override
                        public void run() {

                            System.out.println(
                                    "[A] "
                                            + LocalDateTime.now()
                                            + " | "
                                            + Thread.currentThread().getName()
                            );

                            latch.countDown();
                        }
                    }
            );

            scheduler.schedule(
                    "task-B",
                    "* * * * * ?",
                    new Runnable() {
                        @Override
                        public void run() {

                            System.out.println(
                                    "[B] "
                                            + LocalDateTime.now()
                                            + " | "
                                            + Thread.currentThread().getName()
                            );

                            latch.countDown();
                        }
                    }
            );

            assertTrue(
                    latch.await(10, TimeUnit.SECONDS)
            );

        } finally {
            scheduler.shutdown();
        }
    }

    /**
     * 测试取消任务以后不会继续执行。
     */
    @Test
    void shouldStopExecutingAfterCancel()
            throws Exception {

        final AtomicInteger counter =
                new AtomicInteger(0);

        SimpleScheduler scheduler =
                new SimpleScheduler(2);

        try {

            scheduler.schedule(
                    "cancel-test",
                    "* * * * * ?",
                    new Runnable() {
                        @Override
                        public void run() {

                            int count =
                                    counter.incrementAndGet();

                            System.out.println(
                                    "[cancel-test] execution="
                                            + count
                            );
                        }
                    }
            );

            // 先让任务执行几次
            Thread.sleep(3500);

            scheduler.cancel("cancel-test");

            int countAfterCancel =
                    counter.get();

            System.out.println(
                    "cancelled, count="
                            + countAfterCancel
            );

            // 再等待一段时间
            Thread.sleep(2500);

            // 取消以后不应该继续增加
            assertEquals(
                    countAfterCancel,
                    counter.get()
            );

        } finally {
            scheduler.shutdown();
        }
    }

    /**
     * 测试业务任务抛异常时，
     * SchedulerThread 不应该因此退出。
     */
    @Test
    void shouldSurviveTaskException()
            throws Exception {

        final CountDownLatch latch =
                new CountDownLatch(2);

        SimpleScheduler scheduler =
                new SimpleScheduler(2);

        try {

            scheduler.schedule(
                    "exception-test",
                    "* * * * * ?",
                    new Runnable() {
                        @Override
                        public void run() {

                            System.out.println(
                                    "[exception-test] "
                                            + LocalDateTime.now()
                            );

                            latch.countDown();

                            throw new RuntimeException(
                                    "Test exception"
                            );
                        }
                    }
            );

            /*
             * 如果 SchedulerThread 因为第一次任务异常
             * 而死亡，那么第二次执行就不会发生。
             */
            assertTrue(
                    latch.await(5, TimeUnit.SECONDS)
            );

        } finally {
            scheduler.shutdown();
        }
    }
}

