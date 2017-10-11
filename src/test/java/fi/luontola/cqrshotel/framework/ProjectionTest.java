// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

import com.google.common.base.Stopwatch;
import fi.luontola.cqrshotel.FastTests;
import fi.luontola.cqrshotel.util.Struct;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.Timeout;

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

@Category(FastTests.class)
public class ProjectionTest {

    @Rule
    public final Timeout timeout = Timeout.seconds(1);

    private static final DummyEvent one = new DummyEvent("one");
    private static final DummyEvent two = new DummyEvent("two");
    private static final DummyEvent three = new DummyEvent("three");

    private final EventStore eventStore = new InMemoryEventStore();
    private final SpyProjection projection = new SpyProjection(eventStore);

    @Test
    public void does_nothing_if_no_events() {
        projection.update();

        assertThat(projection.receivedEvents, is(empty()));
    }

    @Test
    public void updates_events_for_all_aggregates() {
        eventStore.saveEvents(UUID.randomUUID(), singletonList(one), EventStore.BEGINNING);
        eventStore.saveEvents(UUID.randomUUID(), singletonList(two), EventStore.BEGINNING);
        projection.update();

        assertThat(projection.receivedEvents, is(asList(one, two)));
    }

    @Test
    public void updates_only_new_events_since_last_update() {
        eventStore.saveEvents(UUID.randomUUID(), singletonList(one), EventStore.BEGINNING);
        eventStore.saveEvents(UUID.randomUUID(), singletonList(two), EventStore.BEGINNING);
        projection.update();
        projection.receivedEvents.clear();

        eventStore.saveEvents(UUID.randomUUID(), singletonList(three), EventStore.BEGINNING);
        projection.update();

        assertThat("new events", projection.receivedEvents, is(singletonList(three)));
    }

    @Test
    public void awaiting_position_blocks_until_the_projection_has_been_updated() throws InterruptedException {
        eventStore.saveEvents(UUID.randomUUID(), singletonList(one), EventStore.BEGINNING);

        new Thread(() -> {
            sleep(5);
            projection.update();
        }).start();
        boolean result = projection.awaitPosition(1, Duration.ofSeconds(1));

        List<DummyEvent> events = new ArrayList<>(projection.receivedEvents); // safe copy to avoid assertion message showing a later value
        assertThat("events", events, is(singletonList(one)));
        assertThat("return value", result, is(true));
    }

    @Test
    public void awaiting_position_returns_false_if_timeout_is_reached() throws InterruptedException {
        boolean result = projection.awaitPosition(1, Duration.ofSeconds(0));

        assertThat("return value", result, is(false));
    }

    @Test
    public void awaiting_position_returns_immediately_if_the_projection_is_already_up_to_date() throws InterruptedException {
        eventStore.saveEvents(UUID.randomUUID(), singletonList(one), EventStore.BEGINNING);
        eventStore.saveEvents(UUID.randomUUID(), singletonList(two), EventStore.BEGINNING);
        projection.update();

        assertThat("when expectation smaller", projection.awaitPosition(1, Duration.ofSeconds(0)), is(true));
        assertThat("when expectation equal", projection.awaitPosition(2, Duration.ofSeconds(0)), is(true));
    }

    @Test
    public void awaiting_position_is_not_blocked_by_a_concurrently_running_update() throws InterruptedException {
        eventStore.saveEvents(UUID.randomUUID(), singletonList(one), EventStore.BEGINNING);
        CountDownLatch updateStarted = new CountDownLatch(1);
        Projection projection = new Projection(eventStore) {
            @EventListener
            private void apply(DummyEvent event) {
                updateStarted.countDown();
                sleep(500);
            }
        };
        new Thread(projection::update).start();
        updateStarted.await();

        Stopwatch stopwatch = Stopwatch.createStarted();
        boolean result = projection.awaitPosition(1, Duration.ofMillis(0));

        assertThat("elapsed time", stopwatch.elapsed(TimeUnit.MILLISECONDS), is(lessThan(100L)));
        assertThat("return value", result, is(false));
    }


    private static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // ignore
        }
    }

    private static class SpyProjection extends Projection {

        public final List<DummyEvent> receivedEvents = new ArrayList<>();

        public SpyProjection(EventStore eventStore) {
            super(eventStore);
        }

        @EventListener
        private void apply(DummyEvent event) {
            receivedEvents.add(event);
        }
    }

    private static class DummyEvent extends Struct implements Event {
        public final String message;

        private DummyEvent(String message) {
            this.message = message;
        }
    }
}