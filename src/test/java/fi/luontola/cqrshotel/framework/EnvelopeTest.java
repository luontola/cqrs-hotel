// Copyright © 2016-2019 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@Tag("fast")
public class EnvelopeTest {

    @BeforeEach
    @AfterEach
    public void resetCause() {
        Envelope.resetContext();
    }

    @Test
    public void message_IDs_are_unique() {
        var m1 = Envelope.newMessage(new DummyMessage());
        var m2 = Envelope.newMessage(new DummyMessage());

        assertThat(m1.messageId, is(notNullValue()));
        assertThat(m1.messageId, is(not(equalTo(m2.messageId))));
    }

    @Test
    public void origin_messages_have_unique_correlation_IDs() {
        var m1 = Envelope.newMessage(new DummyMessage());
        var m2 = Envelope.newMessage(new DummyMessage());

        assertThat(m1.correlationId, is(notNullValue()));
        assertThat(m1.correlationId, is(not(equalTo(m2.correlationId))));
    }

    @Test
    public void origin_messages_do_not_have_causation_IDs() {
        var m1 = Envelope.newMessage(new DummyMessage());

        assertThat(m1.causationId, is(nullValue()));
    }

    @Test
    public void outcome_messages_have_the_same_correlation_ID_as_their_cause() {
        var origin = Envelope.newMessage(new DummyMessage());
        Envelope.setContext(origin);
        var m1 = Envelope.newMessage(new DummyMessage());
        var m2 = Envelope.newMessage(new DummyMessage());

        assertThat(m1.correlationId, is(notNullValue()));
        assertThat(m1.correlationId, is(origin.correlationId));
        assertThat(m2.correlationId, is(origin.correlationId));
    }

    @Test
    public void outcome_messages_have_the_message_ID_of_their_cause_as_their_causation_ID() {
        var origin = Envelope.newMessage(new DummyMessage());
        Envelope.setContext(origin);
        var m1 = Envelope.newMessage(new DummyMessage());
        var m2 = Envelope.newMessage(new DummyMessage());

        assertThat(m1.causationId, is(notNullValue()));
        assertThat(m1.causationId, is(origin.messageId));
        assertThat(m2.causationId, is(origin.messageId));
    }

    @Test
    public void after_resetting_the_context_only_origin_messages_are_created() {
        Envelope.setContext(Envelope.newMessage(new DummyMessage()));
        var m1 = Envelope.newMessage(new DummyMessage());
        assertThat(m1.causationId, is(notNullValue()));

        Envelope.resetContext();

        var m2 = Envelope.newMessage(new DummyMessage());
        assertThat(m2.causationId, is(nullValue()));
    }

    @Test
    public void can_change_the_correlation_ID() {
        var payload = new DummyMessage();
        var original = new Envelope<>(payload, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        var newCorrelationId = UUID.randomUUID();

        var changed = original.withCorrelationId(newCorrelationId);

        assertThat("correlationId", changed.correlationId, is(newCorrelationId));
        // all others say the same
        assertThat("messageId", changed.messageId, is(original.messageId));
        assertThat("causationId", changed.causationId, is(original.causationId));
        assertThat("payload", changed.payload, is(original.payload));
    }

    private class DummyMessage implements Message {
    }
}
