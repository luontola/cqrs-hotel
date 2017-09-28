// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

import fi.luontola.cqrshotel.FastTests;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.Timeout;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

@Category(FastTests.class)
public class SingleThreadedTriggerableWorkerTest {

    @Rule
    public final Timeout timeout = new Timeout(1, TimeUnit.SECONDS);

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final AtomicInteger taskCount = new AtomicInteger(0);

    @After
    public void tearDown() {
        executor.shutdownNow();
    }

    @Test
    public void runs_the_task_when_triggered() throws InterruptedException {
        CountDownLatch taskFinished = new CountDownLatch(1);
        Runnable task = () -> {
            taskCount.incrementAndGet();
            taskFinished.countDown();
        };
        SingleThreadedTriggerableWorker worker = new SingleThreadedTriggerableWorker(task, executor);

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
        SingleThreadedTriggerableWorker worker = new SingleThreadedTriggerableWorker(task, executor);

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
        SingleThreadedTriggerableWorker worker = new SingleThreadedTriggerableWorker(task, executor);

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
        SingleThreadedTriggerableWorker worker = new SingleThreadedTriggerableWorker(task, executor);

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
        SingleThreadedTriggerableWorker worker = new SingleThreadedTriggerableWorker(task, executor);

        worker.trigger();
        taskFinished.await();
        worker.trigger();
        taskFinished.await();
        worker.trigger();
        taskFinished.await();

        assertThat("task count", taskCount.get(), is(3));
    }
}