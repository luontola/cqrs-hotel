// Copyright © 2016 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

import java.util.Arrays;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public abstract class AggregateRootTester {

    protected final UUID id = UUID.randomUUID();
    protected final FakeEventStore eventStore = new FakeEventStore();
    protected Handles commandHandler;

    public void given(Event... events) {
        eventStore.existing = Arrays.asList(events);
    }

    public void when(Command command) {
        commandHandler.handle(command);
    }

    public void then(Event... expectedEvents) {
        assertThat("produced events", eventStore.produced, is(Arrays.asList(expectedEvents)));
    }
}
