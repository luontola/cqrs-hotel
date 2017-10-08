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
    // TODO: call observe() automatically for commands
    // TODO: call observe() automatically for projections

    public static final String HTTP_HEADER = "X-Observed-Position";
    public static final Duration QUERY_TIMEOUT = Duration.ofSeconds(15);
    private static final Logger log = LoggerFactory.getLogger(ObservedPosition.class);

    private final ThreadLocal<Long> observedPosition = new ThreadLocal<>();

    public void observe(long position) {
        Long current = observedPosition.get();
        if (current == null || current < position) {
            observedPosition.set(position);
        }
    }

    public void waitForProjectionToUpdate(Projection projection) {
        // TODO: do this in a single place in the handler chain so that it doesn't need to be added individually for each projection
        try {
            boolean upToDate = projection.awaitPosition(this.get(), QUERY_TIMEOUT);
            if (upToDate) {
                return;
            }
            log.warn("Projection {} not up to date, expected position {} ",
                    projection.getClass().getSimpleName(), observedPosition);
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
