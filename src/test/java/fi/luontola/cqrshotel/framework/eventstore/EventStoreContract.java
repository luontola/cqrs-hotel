// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework.eventstore;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import fi.luontola.cqrshotel.framework.Envelope;
import fi.luontola.cqrshotel.framework.Event;
import fi.luontola.cqrshotel.framework.util.Struct;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

public abstract class EventStoreContract {

    // TODO: use a cursor to search results

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    protected EventStore eventStore;

    @Before
    public final void parentInit() {
        init();
        Assert.assertNotNull("eventStore was not set", eventStore);
    }

    protected abstract void init();

    @Test
    public void saving_new_aggregate() {
        Envelope<Event> one = dummyEvent("one");
        Envelope<Event> two = dummyEvent("two");
        UUID streamId = UUID.randomUUID();
        long start = eventStore.getCurrentPosition();

        eventStore.saveEvents(streamId, Arrays.asList(one, two), EventStore.BEGINNING);

        List<PersistedEvent> events = eventStore.getEventsForStream(streamId);
        assertThat(events, is(Arrays.asList(
                new PersistedEvent(one, streamId, 1, start + 1),
                new PersistedEvent(two, streamId, 2, start + 2))));
    }

    @Test
    public void appending_events_to_existing_aggregate() {
        Envelope<Event> one = dummyEvent("one");
        Envelope<Event> two = dummyEvent("two");
        Envelope<Event> three = dummyEvent("three");
        Envelope<Event> four = dummyEvent("four");
        UUID streamId = UUID.randomUUID();
        long start = eventStore.getCurrentPosition();
        eventStore.saveEvents(streamId, Arrays.asList(one, two), EventStore.BEGINNING);

        eventStore.saveEvents(streamId, Arrays.asList(three, four), 2);

        List<PersistedEvent> events = eventStore.getEventsForStream(streamId);
        assertThat(events, is(Arrays.asList(
                new PersistedEvent(one, streamId, 1, start + 1),
                new PersistedEvent(two, streamId, 2, start + 2),
                new PersistedEvent(three, streamId, 3, start + 3),
                new PersistedEvent(four, streamId, 4, start + 4))));
    }

    @Test
    public void cannot_save_events_if_expecting_wrong_version() {
        Envelope<Event> one = dummyEvent("one");
        Envelope<Event> two = dummyEvent("two");
        Envelope<Event> three = dummyEvent("three");
        Envelope<Event> four = dummyEvent("four");
        UUID streamId = UUID.randomUUID();
        eventStore.saveEvents(streamId, Arrays.asList(one, two), EventStore.BEGINNING);

        thrown.expect(OptimisticLockingException.class);
        thrown.expectMessage("expected version 1 but was 2 for stream " + streamId);
        eventStore.saveEvents(streamId, Arrays.asList(three, four), 1);
    }

    @Test
    public void non_existing_streams_are_reported_as_empty() {
        UUID streamId = UUID.randomUUID();

        List<PersistedEvent> events = eventStore.getEventsForStream(streamId);
        assertThat(events, is(empty()));
    }

    @Test
    public void reports_current_stream_version() {
        UUID streamId = UUID.randomUUID();

        int v0 = eventStore.getCurrentVersion(streamId);
        eventStore.saveEvents(streamId, Arrays.asList(dummyEvent("foo")), v0);
        int v1 = eventStore.getCurrentVersion(streamId);

        assertThat("v0", v0, is(0));
        assertThat("v1", v1, is(1));
    }

    @Test
    public void reports_current_global_position() {
        long pos0 = eventStore.getCurrentPosition();
        eventStore.saveEvents(UUID.randomUUID(), Arrays.asList(dummyEvent("foo")), EventStore.BEGINNING);
        long pos1 = eventStore.getCurrentPosition();

        assertThat(pos1, is(pos0 + 1));
    }

    @Test
    public void global_position_starts_from_one() {
        // ensure at least two events have been saved previously (the SQL database is not emptied between tests)
        eventStore.saveEvents(UUID.randomUUID(),
                Arrays.asList(dummyEvent("one"), dummyEvent("two")),
                EventStore.BEGINNING);

        List<PersistedEvent> sinceBeginning = eventStore.getAllEvents(EventStore.BEGINNING).subList(0, 2);
        List<PersistedEvent> sinceOne = eventStore.getAllEvents(1).subList(0, 1);

        assertThat(sinceBeginning.get(1), is(sinceOne.get(0)));
        assertThat(sinceBeginning.get(0).position, is(1L));
        assertThat(sinceBeginning.get(1).position, is(2L));
    }

    @Test
    public void reading_events_since_a_particular_version() {
        Envelope<Event> one = dummyEvent("one");
        Envelope<Event> two = dummyEvent("two");
        long start = eventStore.getCurrentPosition();
        UUID streamId = UUID.randomUUID();
        eventStore.saveEvents(streamId, Arrays.asList(one, two), EventStore.BEGINNING);

        assertThat("since beginning", eventStore.getEventsForStream(streamId, EventStore.BEGINNING), is(Arrays.asList(
                new PersistedEvent(one, streamId, 1, start + 1),
                new PersistedEvent(two, streamId, 2, start + 2))));
        assertThat("since middle", eventStore.getEventsForStream(streamId, 1), is(Arrays.asList(
                new PersistedEvent(two, streamId, 2, start + 2))));
        assertThat("since end", eventStore.getEventsForStream(streamId, 2), is(empty()));
    }

    @Test
    public void reading_events_from_all_streams() {
        Envelope<Event> one = dummyEvent("one");
        Envelope<Event> two = dummyEvent("two");
        long start = eventStore.getCurrentPosition();
        UUID streamId1 = UUID.randomUUID();
        UUID streamId2 = UUID.randomUUID();
        eventStore.saveEvents(streamId1, Arrays.asList(one), EventStore.BEGINNING);
        eventStore.saveEvents(streamId2, Arrays.asList(two), EventStore.BEGINNING);

        List<PersistedEvent> events = (eventStore.getAllEvents(start));

        assertThat(events, is(Arrays.asList(
                new PersistedEvent(one, streamId1, 1, start + 1),
                new PersistedEvent(two, streamId2, 1, start + 2))));
    }

    @Test
    public void reports_the_global_position_of_the_last_saved_event() {
        Envelope<Event> a = dummyEvent("a");
        Envelope<Event> b1 = dummyEvent("b1");
        Envelope<Event> b2 = dummyEvent("b2");
        UUID streamA = UUID.randomUUID();
        UUID streamB = UUID.randomUUID();
        long posA = eventStore.saveEvents(streamA, Arrays.asList(a), EventStore.BEGINNING);
        long posB = eventStore.saveEvents(streamB, Arrays.asList(b1, b2), EventStore.BEGINNING);

        assertThat(eventStore.getAllEvents(posA - 1).get(0), is(new PersistedEvent(a, streamA, 1, posA)));
        assertThat(eventStore.getAllEvents(posB - 1).get(0), is(new PersistedEvent(b2, streamB, 2, posB)));
    }

    @Test
    public void concurrent_writers_to_same_stream() throws Exception {
        final int BATCH_SIZE = 10;
        final int ITERATIONS = 100;
        UUID streamId = UUID.randomUUID();
        long start = eventStore.getCurrentPosition();
        AtomicInteger taskIdSeq = new AtomicInteger(0);

        repeatInParallel(ITERATIONS, () -> {
            int taskId = taskIdSeq.incrementAndGet();
            List<Envelope<Event>> batch = createBatch(BATCH_SIZE, taskId);

            while (true) {
                try {
                    int version1 = eventStore.getCurrentVersion(streamId);
                    eventStore.saveEvents(streamId, batch, version1);
                    return;
                } catch (OptimisticLockingException e) {
                    // retry
                }
            }
        }, createRuntimeInvariantChecker(BATCH_SIZE));

        List<PersistedEvent> streamEvents = eventStore.getEventsForStream(streamId);
        assertThat("number of saved events", streamEvents.size(), is(BATCH_SIZE * ITERATIONS));
        assertAtomicBatches(BATCH_SIZE, streamEvents);
        List<PersistedEvent> allEvents = eventStore.getAllEvents(start);
        assertThat("global order should equal stream order", allEvents, contains(streamEvents.toArray()));
    }

    @Test
    public void concurrent_writers_to_different_streams() throws Exception {
        final int BATCH_SIZE = 10;
        final int ITERATIONS = 100;
        long start = eventStore.getCurrentPosition();
        AtomicInteger taskIdSeq = new AtomicInteger(0);

        repeatInParallel(ITERATIONS, () -> {
            UUID streamId = UUID.randomUUID();
            int taskId = taskIdSeq.incrementAndGet();
            List<Envelope<Event>> batch = createBatch(BATCH_SIZE, taskId);

            eventStore.saveEvents(streamId, batch, EventStore.BEGINNING);
        }, createRuntimeInvariantChecker(BATCH_SIZE));

        List<PersistedEvent> allEvents = eventStore.getAllEvents(start);
        assertThat("number of saved events", allEvents.size(), is(BATCH_SIZE * ITERATIONS));
        assertAtomicBatches(BATCH_SIZE, allEvents);
    }

    private Runnable createRuntimeInvariantChecker(int batchSize) {
        AtomicLong position = new AtomicLong(eventStore.getCurrentPosition());
        return () -> {
            long pos = position.get();
            List<PersistedEvent> events = eventStore.getAllEvents(pos);
            assertAtomicBatches(batchSize, events);
            position.set(pos + events.size());
        };
    }

    private static void repeatInParallel(int iterations, Runnable task, Runnable invariantChecker) throws Exception {
        final int PARALLELISM = 10;
        ExecutorService executor = Executors.newFixedThreadPool(PARALLELISM + 1);
        Future<?> checker;
        try {
            checker = executor.submit(() -> {
                while (!Thread.interrupted()) {
                    invariantChecker.run();
                    Thread.yield();
                }
            });
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < iterations; i++) {
                futures.add(executor.submit(task));
            }
            for (Future<?> future : futures) {
                // will throw ExecutionException if there was a problem
                future.get();
            }
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
        // will throw ExecutionException if there was a problem
        checker.get(10, TimeUnit.SECONDS);
    }

    private static List<Envelope<Event>> createBatch(int batchSize, int taskId) {
        List<Envelope<Event>> events = new ArrayList<>();
        for (int i = 0; i < batchSize; i++) {
            events.add(dummyEvent(taskId + "." + i));
        }
        return events;
    }

    private static void assertAtomicBatches(int batchSize, List<PersistedEvent> events) {
        if (events.size() % batchSize != 0) {
            throw new AssertionError("incomplete batches found: " + events.size() + " events but " + batchSize + " batch size");
        }
        List<List<PersistedEvent>> batches = new ArrayList<>();
        for (int i = 0; i < events.size() / batchSize; i++) {
            int start = i * batchSize;
            batches.add(events.subList(start, start + batchSize));
        }
        for (List<PersistedEvent> batch : batches) {
            DummyEvent sample = (DummyEvent) batch.get(0).event.payload;
            String prefix = sample.message.substring(0, sample.message.indexOf('.'));
            try {
                for (int i = 0; i < batch.size(); i++) {
                    DummyEvent event = (DummyEvent) batch.get(i).event.payload;
                    assertThat(event.message, is(prefix + "." + i));
                }
            } catch (AssertionError e) {
                throw new AssertionError("non-atomic batch found: " + batch, e);
            }
        }
    }

    private static Envelope<Event> dummyEvent(String message) {
        // fill in all IDs to make sure that they all are saved and loaded correctly
        return new Envelope<>(new DummyEvent(message), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    }

    public static class DummyEvent extends Struct implements Event {
        public final String message;

        @JsonCreator
        public DummyEvent(@JsonProperty("message") String message) {
            this.message = message;
        }
    }
}