// Copyright © 2016 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import fi.luontola.cqrshotel.util.Struct;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public abstract class EventStoreContract {

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
        UUID id = UUID.randomUUID();
        eventStore.saveEvents(id,
                Arrays.asList(new DummyEvent("one"), new DummyEvent("two")),
                EventStore.BEGINNING);

        List<Event> events = eventStore.getEventsForStream(id);
        assertThat(events, is(Arrays.asList(new DummyEvent("one"), new DummyEvent("two"))));
    }

    @Test
    public void appending_events_to_existing_aggregate() {
        UUID id = UUID.randomUUID();
        eventStore.saveEvents(id,
                Arrays.asList(new DummyEvent("one"), new DummyEvent("two")),
                EventStore.BEGINNING);

        eventStore.saveEvents(id,
                Arrays.asList(new DummyEvent("three"), new DummyEvent("four")),
                2);

        List<Event> events = eventStore.getEventsForStream(id);
        assertThat(events, is(Arrays.asList(
                new DummyEvent("one"), new DummyEvent("two"), new DummyEvent("three"), new DummyEvent("four"))));
    }

    @Test
    public void cannot_save_events_if_expecting_wrong_version() {
        UUID id = UUID.randomUUID();
        eventStore.saveEvents(id,
                Arrays.asList(new DummyEvent("one"), new DummyEvent("two")),
                EventStore.BEGINNING);

        thrown.expect(OptimisticLockingException.class);
        thrown.expectMessage("expected version 1 but was 2 for stream " + id);
        eventStore.saveEvents(id,
                Arrays.asList(new DummyEvent("three"), new DummyEvent("four")),
                1);
    }

    @Test
    public void cannot_read_events_from_non_existing_streams() {
        UUID id = UUID.randomUUID();
        thrown.expect(EventStreamNotFoundException.class);
        thrown.expectMessage(id.toString());
        eventStore.getEventsForStream(id);
    }

    @Test
    public void reports_current_stream_version() {
        UUID id = UUID.randomUUID();

        int v0 = eventStore.getCurrentVersion(id);
        eventStore.saveEvents(id, Arrays.asList(new DummyEvent("foo")), v0);
        int v1 = eventStore.getCurrentVersion(id);

        assertThat("v0", v0, is(0));
        assertThat("v1", v1, is(1));
    }

    @Test
    public void reports_current_global_position() {
        long pos0 = eventStore.getCurrentPosition();
        eventStore.saveEvents(UUID.randomUUID(), Arrays.asList(new DummyEvent("foo")), EventStore.BEGINNING);
        long pos1 = eventStore.getCurrentPosition();

        assertThat(pos1, is(pos0 + 1));
    }

    @Test
    public void reading_events_since_a_particular_version() {
        UUID id = UUID.randomUUID();
        eventStore.saveEvents(id,
                Arrays.asList(new DummyEvent("one"), new DummyEvent("two")),
                EventStore.BEGINNING);

        assertThat("since beginning", eventStore.getEventsForStream(id, EventStore.BEGINNING),
                is(Arrays.asList(new DummyEvent("one"), new DummyEvent("two"))));
        assertThat("since middle", eventStore.getEventsForStream(id, 1),
                is(Arrays.asList(new DummyEvent("two"))));
        assertThat("since end", eventStore.getEventsForStream(id, 2),
                is(Arrays.asList()));
    }

    @Test
    public void reading_events_from_all_streams() {
        long position = eventStore.getCurrentPosition();
        UUID id1 = UUID.randomUUID();
        eventStore.saveEvents(id1, Arrays.asList(new DummyEvent("one")), EventStore.BEGINNING);
        UUID id2 = UUID.randomUUID();
        eventStore.saveEvents(id2, Arrays.asList(new DummyEvent("two")), EventStore.BEGINNING);

        List<Event> events = eventStore.getAllEvents(position);

        assertThat(events, is(Arrays.asList(new DummyEvent("one"), new DummyEvent("two"))));
    }

    @Test
    public void reports_the_global_position_of_the_last_saved_event() {
        long posA = eventStore.saveEvents(UUID.randomUUID(), Arrays.asList(new DummyEvent("a1"), new DummyEvent("a2")), EventStore.BEGINNING);
        long posB = eventStore.saveEvents(UUID.randomUUID(), Arrays.asList(new DummyEvent("b")), EventStore.BEGINNING);
        long posC = eventStore.saveEvents(UUID.randomUUID(), Arrays.asList(new DummyEvent("c")), EventStore.BEGINNING);

        assertThat("since a", eventStore.getAllEvents(posA),
                is(Arrays.asList(new DummyEvent("b"), new DummyEvent("c"))));
        assertThat("since b", eventStore.getAllEvents(posB),
                is(Arrays.asList(new DummyEvent("c"))));
        assertThat("since c", eventStore.getAllEvents(posC),
                is(Arrays.asList()));
    }

    // TODO: use a cursor to search results
    // TODO: concurrency test: multiple writers to same stream, each commit is atomic
    // TODO: concurrency test: multiple writers to different streams, each commit is atomic


    public static class DummyEvent extends Struct implements Event {
        public final String message;

        @JsonCreator
        public DummyEvent(@JsonProperty("message") String message) {
            this.message = message;
        }
    }
}