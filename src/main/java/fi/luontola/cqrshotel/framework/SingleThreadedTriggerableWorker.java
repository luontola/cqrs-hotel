// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SingleThreadedTriggerableWorker {

    private static final Logger log = LoggerFactory.getLogger(SingleThreadedTriggerableWorker.class);

    private final ExecutorService executor;
    private final BlockingQueue<Runnable> availableTasks;
    private final UncaughtExceptionHandler uncaughtExceptionHandler;

    public SingleThreadedTriggerableWorker(Runnable task) {
        this(task, exception -> {
            log.error("Uncaught exception in worker thread", exception);
        });
    }

    public SingleThreadedTriggerableWorker(Runnable task, UncaughtExceptionHandler uncaughtExceptionHandler) {
        this.uncaughtExceptionHandler = uncaughtExceptionHandler;
        executor = Executors.newFixedThreadPool(1);
        availableTasks = new ArrayBlockingQueue<>(1);
        availableTasks.add(task);
    }

    public void trigger() {
        Runnable task = availableTasks.poll();
        if (task != null) {
            executor.submit(() -> {
                availableTasks.add(task);
                try {
                    task.run();
                } catch (Throwable t) {
                    uncaughtExceptionHandler.uncaughtException(t);
                }
            });
        }
    }

    public void shutdown() {
        executor.shutdown();
    }

    public void awaitTermination(Duration timeout) throws InterruptedException {
        executor.awaitTermination(timeout.toNanos(), TimeUnit.NANOSECONDS);
    }

    public interface UncaughtExceptionHandler {
        void uncaughtException(Throwable exception);
    }
}
