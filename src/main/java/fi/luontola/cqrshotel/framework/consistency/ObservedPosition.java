// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework.consistency;

import fi.luontola.cqrshotel.framework.Projection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public class ObservedPosition {

    // TODO: tests for this class

    public static final String HTTP_HEADER = "X-Observed-Position";
    private static final Logger log = LoggerFactory.getLogger(ObservedPosition.class);

    private final ThreadLocal<Long> observedPosition = new ThreadLocal<>();
    private final Duration queryTimeout;

    public ObservedPosition(Duration queryTimeout) {
        this.queryTimeout = queryTimeout;
    }

    public void observe(long position) {
        Long current = observedPosition.get();
        if (current == null || current < position) {
            observedPosition.set(position);
        }
    }

    public void waitForProjectionToUpdate(Projection projection) {
        try {
            long expectedPosition = this.get();
            boolean upToDate = projection.awaitPosition(expectedPosition, queryTimeout);
            if (upToDate) {
                return;
            }
            log.warn("Projection {} not up to date, expected position {} ",
                    projection.getClass().getSimpleName(), expectedPosition);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        throw new ReadModelNotUpToDateException();
    }

    public long get() {
        Long value = observedPosition.get();
        return value == null ? 0 : value;
    }

    public void reset() {
        observedPosition.remove();
    }
}
