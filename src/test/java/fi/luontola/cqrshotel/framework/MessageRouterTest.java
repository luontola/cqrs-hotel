// Copyright © 2016 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class MessageRouterTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void delegates_messages_to_the_handlers_by_message_type() {
        DummyEvent1Handler handler1 = new DummyEvent1Handler();
        DummyEvent2Handler handler2 = new DummyEvent2Handler();
        MessageRouter<Event> composite = new MessageRouter<>();
        composite.register(DummyEvent1.class, handler1);
        composite.register(DummyEvent2.class, handler2);
        DummyEvent1 message1 = new DummyEvent1();
        DummyEvent2 message2 = new DummyEvent2();

        composite.handle(message1);
        composite.handle(message2);

        assertThat("handler1.received", handler1.received, is(message1));
        assertThat("handler2.received", handler2.received, is(message2));
    }

    @Test
    public void cannot_register_two_handlers_for_the_same_message() {
        DummyEvent1Handler handler1 = new DummyEvent1Handler();
        DummyEvent1Handler handler2 = new DummyEvent1Handler();
        MessageRouter<Event> composite = new MessageRouter<>();

        composite.register(DummyEvent1.class, handler1);

        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("already registered");
        composite.register(DummyEvent1.class, handler2);
    }

    @Test
    public void fails_for_messages_not_registered() {
        MessageRouter<Event> composite = new MessageRouter<>();

        DummyEvent1 message = new DummyEvent1();

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("no handler");
        composite.handle(message);
    }

    private static class DummyEvent1 implements Event {
    }

    private static class DummyEvent2 implements Event {
    }

    private static class DummyEvent1Handler implements Handles<DummyEvent1> {
        DummyEvent1 received;

        @Override
        public void handle(DummyEvent1 event) {
            this.received = event;
        }
    }

    private static class DummyEvent2Handler implements Handles<DummyEvent2> {
        DummyEvent2 received;

        @Override
        public void handle(DummyEvent2 event) {
            this.received = event;
        }
    }
}