// Copyright © 2016-2019 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

import fi.luontola.cqrshotel.FastTests;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@Category(FastTests.class)
public class CompositeHandlerTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void delegates_messages_to_the_handlers_by_message_type() {
        var handler1 = new Handler1();
        var handler2 = new Handler2();
        var composite = new CompositeHandler<Message, String>();
        composite.register(Message1.class, handler1);
        composite.register(Message2.class, handler2);
        var message1 = new Message1();
        var message2 = new Message2();

        var result1 = composite.handle(message1);
        var result2 = composite.handle(message2);

        assertThat("handler1.received", handler1.received, is(message1));
        assertThat("result1", result1, is("one"));
        assertThat("result2", result2, is("two"));
        assertThat("handler2.received", handler2.received, is(message2));
    }

    @Test
    public void cannot_register_two_handlers_for_the_same_message() {
        var handler1 = new Handler1();
        var handler2 = new Handler1();
        var composite = new CompositeHandler<Message, String>();

        composite.register(Message1.class, handler1);

        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("already registered");
        composite.register(Message1.class, handler2);
    }

    @Test
    public void fails_for_messages_not_registered() {
        var composite = new CompositeHandler<Message, String>();

        var message = new Message1();

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("no handler");
        composite.handle(message);
    }

    private static class Message1 implements Message {
    }

    private static class Message2 implements Message {
    }

    private static class Handler1 implements Handler<Message1, String> {
        Message1 received;

        @Override
        public String handle(Message1 message) {
            this.received = message;
            return "one";
        }
    }

    private static class Handler2 implements Handler<Message2, String> {
        Message2 received;

        @Override
        public String handle(Message2 message) {
            this.received = message;
            return "two";
        }
    }
}