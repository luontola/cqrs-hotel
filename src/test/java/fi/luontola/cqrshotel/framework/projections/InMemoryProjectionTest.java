// Copyright © 2016-2019 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework.projections;

import com.google.common.base.Stopwatch;
import fi.luontola.cqrshotel.framework.Envelope;
import fi.luontola.cqrshotel.framework.Event;
import fi.luontola.cqrshotel.framework.eventstore.EventStore;
import fi.luontola.cqrshotel.framework.eventstore.InMemoryEventStore;
import fi.luontola.cqrshotel.framework.util.Struct;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

@Tag("fast")
public class InMemoryProjectionTest {

    private static final Duration testTimeout = Duration.ofSeconds(1);

    private static final Envelope<Event> one = dummyEvent("one");
    private static final Envelope<Event> two = dummyEvent("two");
    private static final Envelope<Event> three = dummyEvent("three");

    private final EventStore eventStore = new InMemoryEventStore();
    private final SpyProjection projection = new SpyProjection();
    private final InMemoryProjection updater = new InMemoryProjection(projection, eventStore);

    @Test
    public void does_nothing_if_no_events() {
        assertTimeoutPreemptively(testTimeout, () -> {
            updater.update();

            assertThat(projection.receivedEvents, is(empty()));
        });
    }

    @Test
    public void updates_events_for_all_aggregates() {
        assertTimeoutPreemptively(testTimeout, () -> {
            eventStore.saveEvents(UUID.randomUUID(), singletonList(one), EventStore.BEGINNING);
            eventStore.saveEvents(UUID.randomUUID(), singletonList(two), EventStore.BEGINNING);
            updater.update();

            assertThat(projection.receivedEvents, is(asList(one.payload, two.payload)));
        });
    }

    @Test
    public void updates_only_new_events_since_last_update() {
        assertTimeoutPreemptively(testTimeout, () -> {
            eventStore.saveEvents(UUID.randomUUID(), singletonList(one), EventStore.BEGINNING);
            eventStore.saveEvents(UUID.randomUUID(), singletonList(two), EventStore.BEGINNING);
            updater.update();
            projection.receivedEvents.clear();

            eventStore.saveEvents(UUID.randomUUID(), singletonList(three), EventStore.BEGINNING);
            updater.update();

            assertThat("new events", projection.receivedEvents, is(singletonList(three.payload)));
        });
    }

    @Test
    public void awaiting_position_blocks_until_the_projection_has_been_updated() throws InterruptedException {
        assertTimeoutPreemptively(testTimeout, () -> {
            eventStore.saveEvents(UUID.randomUUID(), singletonList(one), EventStore.BEGINNING);

            new Thread(() -> {
                sleep(5);
                updater.update();
            }).start();
            var result = updater.awaitPosition(1, Duration.ofSeconds(1));

            List<Event> events = new ArrayList<>(projection.receivedEvents); // safe copy to avoid assertion message showing a later value
            assertThat("events", events, is(singletonList(one.payload)));
            assertThat("return value", result, is(true));
        });
    }

    @Test
    public void awaiting_position_returns_false_if_timeout_is_reached() throws InterruptedException {
        assertTimeoutPreemptively(testTimeout, () -> {
            var result = updater.awaitPosition(1, Duration.ofSeconds(0));

            assertThat("return value", result, is(false));
        });
    }

    @Test
    public void awaiting_position_returns_immediately_if_the_projection_is_already_up_to_date() throws InterruptedException {
        assertTimeoutPreemptively(testTimeout, () -> {
            eventStore.saveEvents(UUID.randomUUID(), singletonList(one), EventStore.BEGINNING);
            eventStore.saveEvents(UUID.randomUUID(), singletonList(two), EventStore.BEGINNING);
            updater.update();

            assertThat("when expectation smaller", updater.awaitPosition(1, Duration.ofSeconds(0)), is(true));
            assertThat("when expectation equal", updater.awaitPosition(2, Duration.ofSeconds(0)), is(true));
        });
    }

    @Test
    public void awaiting_position_is_not_blocked_by_a_concurrently_running_update() throws InterruptedException {
        assertTimeoutPreemptively(testTimeout, () -> {
            eventStore.saveEvents(UUID.randomUUID(), singletonList(one), EventStore.BEGINNING);
            var updateStarted = new CountDownLatch(1);
            var projection = new InMemoryProjection(event -> {
                updateStarted.countDown();
                sleep(500);
            }, eventStore);
            new Thread(projection::update).start();
            updateStarted.await();

            var stopwatch = Stopwatch.createStarted();
            var result = projection.awaitPosition(1, Duration.ofMillis(0));

            assertThat("elapsed time", stopwatch.elapsed(TimeUnit.MILLISECONDS), is(lessThan(100L)));
            assertThat("return value", result, is(false));
        });
    }


    private static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }


    private static Envelope<Event> dummyEvent(String message) {
        return Envelope.newMessage(new DummyEvent(message));
    }

    private static class SpyProjection implements Projection {

        public final List<Event> receivedEvents = new ArrayList<>();

        public void apply(Envelope<Event> event) {
            receivedEvents.add(event.payload);
        }
    }

    private static class DummyEvent extends Struct implements Event {
        public final String message;

        private DummyEvent(String message) {
            this.message = message;
        }
    }
}