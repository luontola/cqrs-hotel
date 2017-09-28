// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

import java.util.concurrent.ExecutorService;

public class SingleThreadedTriggerableWorker {

    private final Runnable task;
    private final ExecutorService executor;
    private volatile boolean dirty = false;

    public SingleThreadedTriggerableWorker(Runnable task, ExecutorService executor) {
        this.task = task;
        this.executor = executor;
    }

    public void trigger() {
        if (!dirty) {
            // XXX: race condition
            dirty = true;
            executor.submit(() -> {
                dirty = false;
                task.run();
            });
        }
    }
}
