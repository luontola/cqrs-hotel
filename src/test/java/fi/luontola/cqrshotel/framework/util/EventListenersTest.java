// Copyright © 2016-2019 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework.util;

import fi.luontola.cqrshotel.FastTests;
import fi.luontola.cqrshotel.framework.Event;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import static fi.luontola.cqrshotel.framework.util.EventListeners.Requirements.MUST_BE_PRIVATE;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

@Category(FastTests.class)
public class EventListenersTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void sends_events_to_event_listeners() {
        List<Event> receivedEvents = new ArrayList<>();
        class Target {
            @EventListener
            public void apply(DummyEvent1 event) {
                receivedEvents.add(event);
            }

            @EventListener
            public void apply(DummyEvent2 event) {
                receivedEvents.add(event);
            }
        }
        var eventListeners = EventListeners.of(new Target());

        eventListeners.send(new DummyEvent1());
        eventListeners.send(new DummyEvent2());

        assertThat(receivedEvents, is(asList(new DummyEvent1(), new DummyEvent2())));
    }

    @Test
    public void silently_ignores_events_which_are_not_listened() {
        class Target {
        }
        var eventListeners = EventListeners.of(new Target());

        eventListeners.send(new DummyEvent1());
    }

    @Test
    public void rethrows_exceptions_from_listeners() {
        class Target {
            @EventListener
            public void apply(DummyEvent1 event) {
                throw new IllegalArgumentException("dummy exception");
            }
        }
        var eventListeners = EventListeners.of(new Target());

        thrown.expect(RuntimeException.class);
        thrown.expectMessage("event listener failed for event: EventListenersTest.DummyEvent1[]");
        thrown.expectCause(is(instanceOf(InvocationTargetException.class)));
        eventListeners.send(new DummyEvent1());
    }


    @Test
    public void rejects_listeners_with_no_arguments() {
        class Target {
            @EventListener
            public void apply() {
            }
        }

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("expected method to take exactly one parameter");
        thrown.expectMessage("Target.apply()");
        EventListeners.of(new Target());
    }

    @Test
    public void rejects_listeners_with_more_than_one_argument() {
        class Target {
            @EventListener
            public void apply(DummyEvent1 foo, DummyEvent1 bar) {
            }
        }

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("expected method to take exactly one parameter");
        thrown.expectMessage("Target.apply(");
        EventListeners.of(new Target());
    }

    @Test
    public void rejects_listeners_with_non_event_argument() {
        class Target {
            @EventListener
            public void apply(String s) {
            }
        }

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("expected method to take an event parameter");
        thrown.expectMessage("Target.apply(");
        EventListeners.of(new Target());
    }

    @Test
    public void optionally_requires_listeners_to_be_private() {
        class TargetPrivate {
            @EventListener
            private void apply(DummyEvent1 event) {
            }
        }
        class TargetPublic {
            @EventListener
            public void apply(DummyEvent1 event) {
            }
        }

        EventListeners.of(new TargetPrivate(), MUST_BE_PRIVATE);

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("expected method to be private");
        thrown.expectMessage("TargetPublic.apply(");
        EventListeners.of(new TargetPublic(), MUST_BE_PRIVATE);
    }


    private static class DummyEvent1 extends Struct implements Event {
    }

    private static class DummyEvent2 extends Struct implements Event {
    }
}
