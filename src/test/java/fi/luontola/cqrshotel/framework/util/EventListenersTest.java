// Copyright © 2016-2019 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework.util;

import fi.luontola.cqrshotel.framework.Event;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import static fi.luontola.cqrshotel.framework.util.EventListeners.Requirements.MUST_BE_PRIVATE;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("fast")
public class EventListenersTest {

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

        var e = assertThrows(RuntimeException.class, () -> {
            eventListeners.send(new DummyEvent1());
        });
        assertThat(e.getMessage(), is("event listener failed for event: EventListenersTest.DummyEvent1[]"));
        assertThat(e.getCause(), is(instanceOf(InvocationTargetException.class)));
    }


    @Test
    public void rejects_listeners_with_no_arguments() {
        class Target {
            @EventListener
            public void apply() {
            }
        }

        var e = assertThrows(IllegalArgumentException.class, () -> {
            EventListeners.of(new Target());
        });
        assertThat(e.getMessage(), containsString("expected method to take exactly one parameter"));
        assertThat(e.getMessage(), containsString("Target.apply()"));
    }

    @Test
    public void rejects_listeners_with_more_than_one_argument() {
        class Target {
            @EventListener
            public void apply(DummyEvent1 foo, DummyEvent1 bar) {
            }
        }

        var e = assertThrows(IllegalArgumentException.class, () -> {
            EventListeners.of(new Target());
        });
        assertThat(e.getMessage(), containsString("expected method to take exactly one parameter"));
        assertThat(e.getMessage(), containsString("Target.apply("));
    }

    @Test
    public void rejects_listeners_with_non_event_argument() {
        class Target {
            @EventListener
            public void apply(String s) {
            }
        }

        var e = assertThrows(IllegalArgumentException.class, () -> {
            EventListeners.of(new Target());
        });
        assertThat(e.getMessage(), containsString("expected method to take an event parameter"));
        assertThat(e.getMessage(), containsString("Target.apply("));
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

        var e = assertThrows(IllegalArgumentException.class, () -> {
            EventListeners.of(new TargetPublic(), MUST_BE_PRIVATE);
        });
        assertThat(e.getMessage(), containsString("expected method to be private"));
        assertThat(e.getMessage(), containsString("TargetPublic.apply("));
    }


    private static class DummyEvent1 extends Struct implements Event {
    }

    private static class DummyEvent2 extends Struct implements Event {
    }
}
