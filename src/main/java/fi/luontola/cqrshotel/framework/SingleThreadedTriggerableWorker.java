// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

public class SingleThreadedTriggerableWorker {

    private final BlockingQueue<Runnable> availableTasks;
    private final ExecutorService executor;

    public SingleThreadedTriggerableWorker(Runnable task, ExecutorService executor) {
        this.availableTasks = new ArrayBlockingQueue<>(1);
        this.availableTasks.add(task);
        this.executor = executor;
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
}
