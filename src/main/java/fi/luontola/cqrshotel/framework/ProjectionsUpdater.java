// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProjectionsUpdater {

    private final List<SingleThreadedTriggerableWorker> workers = new ArrayList<>();

    public ProjectionsUpdater(Projection... projections) {
        this(Arrays.asList(projections));
    }

    public ProjectionsUpdater(List<Projection> projections) {
        for (Projection projection : projections) {
            workers.add(new SingleThreadedTriggerableWorker(projection::update));
        }
    }

    public void updateAll() {
        for (SingleThreadedTriggerableWorker worker : workers) {
            worker.trigger();
        }
    }

    public void shutdown() {
        for (SingleThreadedTriggerableWorker worker : workers) {
            worker.shutdown();
        }
    }
}
