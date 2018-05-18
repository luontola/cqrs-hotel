// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Projections are the way for reading and analyzing event-sourced data.
 * Their {@link EventListener event listener} methods will be called for every {@link Event event} in the system.
 * This way projections can construct a read-optimized model from the event data
 * for the purpose of serving {@link Query queries} efficiently.
 */
public class InMemoryProjectionUpdater {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Projection projection;
    private final EventStore eventStore;
    private final PriorityBlockingQueue<Waiter> waiters = new PriorityBlockingQueue<>();
    private volatile long position = EventStore.BEGINNING;

    public InMemoryProjectionUpdater(Projection projection, EventStore eventStore) {
        this.projection = projection;
        this.eventStore = eventStore;
    }

    public String getProjectionName() {
        return projection.getProjectionName();
    }

    public final long getPosition() {
        return position;
    }

    public synchronized final void update() {
        List<Envelope<Event>> events = eventStore.getAllEvents(position);
        if (!events.isEmpty()) {
            log.debug("Updating projection with {} events since position {}", events.size(), position);
        }
        for (Envelope<Event> event : events) {
            projection.apply(event.payload);
            position++;
            notifyWaiters();
        }
    }

    private void notifyWaiters() {
        while (true) {
            Waiter head = waiters.peek();
            if (head == null || head.expectedPosition > position) {
                return; // no expired waiters
            }
            // remove and notify the first waiter
            head.positionReached.countDown();
            waiters.remove(head);
        }
    }

    /**
     * Blocks the current thread until the projection is updated to include
     * events up to and including {@code expectedPosition} or the timeout expires.
     *
     * @return {@code true} if this projection has reached the expected position
     * and {@code false} if the waiting time elapsed before that
     */
    public final boolean awaitPosition(long expectedPosition, Duration timeout) throws InterruptedException {
        // quick path in case no waiting is needed
        if (position >= expectedPosition) {
            return true;
        }

        // slow path, waiting probably needed
        Waiter waiter = new Waiter(expectedPosition);
        waiters.put(waiter);

        // double check after registering our waiter, in case the position was updated concurrently
        // (it's important that we change `waiters` before reading `position`; the updater does it in the opposite order)
        if (position >= expectedPosition) {
            waiters.remove(waiter);
            return true;
        }

        boolean positionReached = waiter.positionReached.await(timeout.toNanos(), TimeUnit.NANOSECONDS);
        if (!positionReached) {
            waiters.remove(waiter); // we timed out; let the waiter object be garbage collected
        }
        return positionReached;
    }


    private static class Waiter implements Comparable<Waiter> {

        final CountDownLatch positionReached = new CountDownLatch(1);
        final long expectedPosition;

        Waiter(long expectedPosition) {
            this.expectedPosition = expectedPosition;
        }

        @Override
        public int compareTo(Waiter that) {
            return Long.compare(this.expectedPosition, that.expectedPosition);
        }
    }
}
