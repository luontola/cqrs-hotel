// Copyright © 2016-2019 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("fast")
public class CompositeHandlerTest {

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

        var e = assertThrows(IllegalStateException.class, () -> {
            composite.register(Message1.class, handler2);
        });
        assertThat(e.getMessage(), containsString("already registered"));
    }

    @Test
    public void fails_for_messages_not_registered() {
        var composite = new CompositeHandler<Message, String>();

        var message = new Message1();

        var e = assertThrows(IllegalArgumentException.class, () -> {
            composite.handle(message);
        });
        assertThat(e.getMessage(), containsString("no handler"));
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