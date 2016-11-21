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
                EventStore.NEW_STREAM);

        List<Event> events = eventStore.getEventsForStream(id);
        assertThat(events, is(Arrays.asList(new DummyEvent("one"), new DummyEvent("two"))));
    }

    @Test
    public void appending_events_to_existing_aggregate() {
        UUID id = UUID.randomUUID();
        eventStore.saveEvents(id,
                Arrays.asList(new DummyEvent("one"), new DummyEvent("two")),
                EventStore.NEW_STREAM);

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
                EventStore.NEW_STREAM);

        thrown.expect(OptimisticLockingException.class);
        thrown.expectMessage("expected version 1 but was 2 for stream " + id);
        eventStore.saveEvents(id,
                Arrays.asList(new DummyEvent("three"), new DummyEvent("four")),
                1);
    }

    @Test
    public void cannot_read_events_for_non_existing_aggregate() {
        UUID id = UUID.randomUUID();
        thrown.expect(EventStreamNotFoundException.class);
        thrown.expectMessage(id.toString());
        eventStore.getEventsForStream(id);
    }


    public static class DummyEvent extends Struct implements Event {
        public final String message;

        @JsonCreator
        public DummyEvent(@JsonProperty("message") String message) {
            this.message = message;
        }
    }
}