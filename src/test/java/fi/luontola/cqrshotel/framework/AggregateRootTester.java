// Copyright © 2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

import fi.luontola.cqrshotel.reservation.events.LineItemCreated;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public abstract class AggregateRootTester {

    protected final UUID id = UUID.randomUUID();
    protected final FakeEventStore eventStore = new FakeEventStore();
    protected Handler<? extends Command, Void> commandHandler;

    public void given(Event... events) {
        eventStore.existing = Arrays.asList(events);
    }

    public void when(Command command) {
        Handler unchecked = commandHandler;
        unchecked.handle(command);
    }

    public void then(Event... expectedEvents) {
        assertThat(producedEvents(), is(Arrays.asList(expectedEvents)));
    }

    public void then(Predicate<Event> filter, LineItemCreated... expectedEvents) {
        List<Event> events = producedEvents().stream()
                .filter(filter)
                .collect(toList());
        assertThat(events, is(Arrays.asList(expectedEvents)));
    }

    public List<Event> producedEvents() {
        return eventStore.produced;
    }
}
