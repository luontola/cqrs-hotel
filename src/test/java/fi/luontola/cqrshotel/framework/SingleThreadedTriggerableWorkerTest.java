// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

import fi.luontola.cqrshotel.FastTests;
import fi.luontola.cqrshotel.framework.SingleThreadedTriggerableWorker.UncaughtExceptionHandler;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.Timeout;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;

@Category(FastTests.class)
public class SingleThreadedTriggerableWorkerTest {

    @Rule
    public final Timeout timeout = new Timeout(1, TimeUnit.SECONDS);

    private final AtomicInteger taskCount = new AtomicInteger(0);
    private SingleThreadedTriggerableWorker worker;

    @After
    public void tearDown() {
        if (worker != null) {
            worker.shutdown();
        }
    }

    @Test
    public void runs_the_task_when_triggered() throws InterruptedException {
        CountDownLatch taskFinished = new CountDownLatch(1);
        Runnable task = () -> {
            taskCount.incrementAndGet();
            taskFinished.countDown();
        };
        worker = new SingleThreadedTriggerableWorker(task);

        worker.trigger();
        taskFinished.await();

        assertThat("task count", taskCount.get(), is(1));
    }

    @Test
    public void runs_the_task_in_a_background_thread() throws InterruptedException {
        Thread[] taskThread = new Thread[1];
        CountDownLatch taskFinished = new CountDownLatch(1);
        Runnable task = () -> {
            taskThread[0] = Thread.currentThread();
            taskFinished.countDown();
        };
        worker = new SingleThreadedTriggerableWorker(task);

        worker.trigger();
        taskFinished.await();

        assertThat("task thread", taskThread[0], is(notNullValue()));
        assertThat("task thread", taskThread[0], is(not(Thread.currentThread())));
    }

    @Test
    public void reruns_when_triggered_after_task_started() throws InterruptedException {
        CountDownLatch oneTaskStarted = new CountDownLatch(1);
        CountDownLatch twoTasksFinished = new CountDownLatch(2);
        Runnable task = () -> {
            taskCount.incrementAndGet();
            oneTaskStarted.countDown();
            twoTasksFinished.countDown();
        };
        worker = new SingleThreadedTriggerableWorker(task);

        worker.trigger();
        oneTaskStarted.await();
        worker.trigger();
        twoTasksFinished.await();

        assertThat("task count", taskCount.get(), is(2));
    }

    @Test
    public void reruns_only_once_when_triggered_many_times_after_task_started() throws InterruptedException {
        CountDownLatch firstTaskStarted = new CountDownLatch(1);
        CountDownLatch manyTasksTriggered = new CountDownLatch(1);
        CountDownLatch twoTasksFinished = new CountDownLatch(2);
        Runnable task = () -> {
            taskCount.incrementAndGet();
            firstTaskStarted.countDown();
            try {
                manyTasksTriggered.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            twoTasksFinished.countDown();
        };
        worker = new SingleThreadedTriggerableWorker(task);

        worker.trigger();
        firstTaskStarted.await();
        worker.trigger();
        worker.trigger();
        worker.trigger();
        worker.trigger();
        manyTasksTriggered.countDown();
        twoTasksFinished.await();
        Thread.yield(); // wait a moment in case more tasks are run

        assertThat("task count", taskCount.get(), is(2));
    }

    @Test
    public void reruns_many_times_when_triggered_after_task_finished() throws BrokenBarrierException, InterruptedException {
        CyclicBarrier taskFinished = new CyclicBarrier(2);
        Runnable task = () -> {
            taskCount.incrementAndGet();
            try {
                taskFinished.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                throw new RuntimeException(e);
            }
        };
        worker = new SingleThreadedTriggerableWorker(task);

        worker.trigger();
        taskFinished.await();
        worker.trigger();
        taskFinished.await();
        worker.trigger();
        taskFinished.await();

        assertThat("task count", taskCount.get(), is(3));
    }

    @Test
    public void runs_only_a_single_task_at_a_time() throws InterruptedException {
        AtomicInteger concurrentTasks = new AtomicInteger(0);
        AtomicInteger maxConcurrentTasks = new AtomicInteger(0);
        Runnable task = () -> {
            concurrentTasks.incrementAndGet();
            maxConcurrentTasks.updateAndGet(value -> Math.max(value, concurrentTasks.get()));
            taskCount.incrementAndGet();
            Thread.yield();
            concurrentTasks.decrementAndGet();
        };
        worker = new SingleThreadedTriggerableWorker(task);

        while (taskCount.get() < 10 && !Thread.currentThread().isInterrupted()) {
            worker.trigger();
        }

        assertThat("max concurrent tasks", maxConcurrentTasks.get(), is(1));
    }

    @Test
    public void logs_uncaught_exceptions() throws InterruptedException {
        CountDownLatch handlerCalled = new CountDownLatch(1);
        Throwable[] actualException = new Throwable[1];
        UncaughtExceptionHandler exceptionHandler = (e) -> {
            actualException[0] = e;
            handlerCalled.countDown();
        };
        RuntimeException expectedException = new RuntimeException("dummy");
        Runnable task = () -> {
            throw expectedException;
        };
        worker = new SingleThreadedTriggerableWorker(task, exceptionHandler);

        worker.trigger();
        handlerCalled.await();

        assertThat(actualException[0], is(sameInstance(expectedException)));
    }
}
