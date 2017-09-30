// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

public abstract class Projection {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final EventListeners eventListeners;
    private final EventStore eventStore;
    private long position = EventStore.BEGINNING;

    public Projection(EventStore eventStore) {
        this.eventListeners = EventListeners.of(this);
        this.eventStore = eventStore;
    }

    public synchronized final void update() {
        List<Event> events = eventStore.getAllEvents(position);
        if (!events.isEmpty()) {
            log.debug("Updating projection with {} events since position {}", events.size(), position);
        }
        for (Event event : events) {
            eventListeners.send(event);
            position++;
        }
        notifyAll();
    }

    /**
     * Blocks the current thread until the projection is updated to include
     * events up to and including {@code expectedPosition} or the timeout expires.
     *
     * @return {@code true} if this projection has reached the expected position
     * and {@code false} if the waiting time elapsed before that
     */
    public synchronized final boolean awaitPosition(long expectedPosition, Duration timeout) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            if (position >= expectedPosition) {
                return true;
            }
            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0) {
                return false;
            }
            wait(remaining);
        }
    }
}
