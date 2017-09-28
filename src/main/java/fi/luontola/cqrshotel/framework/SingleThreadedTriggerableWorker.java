// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SingleThreadedTriggerableWorker {

    private final ExecutorService executor;
    private final BlockingQueue<Runnable> availableTasks;

    public SingleThreadedTriggerableWorker(Runnable task) {
        executor = Executors.newFixedThreadPool(1);
        availableTasks = new ArrayBlockingQueue<>(1);
        availableTasks.add(task);
    }

    public void trigger() {
        Runnable task = availableTasks.poll();
        if (task != null) {
            executor.submit(() -> {
                availableTasks.add(task);
                task.run();
            });
        }
    }

    public void shutdown() {
        executor.shutdown();
    }
}
