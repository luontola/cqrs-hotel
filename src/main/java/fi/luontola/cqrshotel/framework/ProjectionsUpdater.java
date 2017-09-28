// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

import java.util.Arrays;
import java.util.List;

public class ProjectionsUpdater {

    private final List<Projection> projections;

    public ProjectionsUpdater(Projection... projections) {
        this(Arrays.asList(projections));
    }

    public ProjectionsUpdater(List<Projection> projections) {
        this.projections = projections;
    }

    public void updateAll() {
        for (Projection projection : projections) {
            projection.update();
        }
    }
}
