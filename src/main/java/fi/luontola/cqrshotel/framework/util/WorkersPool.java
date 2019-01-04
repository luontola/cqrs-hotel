// Copyright © 2016-2019 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework.util;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class WorkersPool {

    private final List<SingleThreadedTriggerableWorker> workers = new ArrayList<>();

    public WorkersPool(List<Runnable> updaters) {
        for (var updater : updaters) {
            workers.add(new SingleThreadedTriggerableWorker(updater));
        }
    }

    public void updateAll() {
        for (var worker : workers) {
            worker.trigger();
        }
    }

    public void shutdown(Duration timeout) throws InterruptedException {
        for (var worker : workers) {
            worker.shutdown();
        }
        for (var worker : workers) {
            worker.awaitTermination(timeout);
        }
    }
}
