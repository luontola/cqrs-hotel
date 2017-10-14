// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

import fi.luontola.cqrshotel.FastTests;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@Category(FastTests.class)
public class EnvelopeTest {

    @Before
    @After
    public void resetCause() {
        Envelope.resetContext();
    }

    @Test
    public void message_IDs_are_unique() {
        Envelope<DummyMessage> m1 = Envelope.newMessage(new DummyMessage());
        Envelope<DummyMessage> m2 = Envelope.newMessage(new DummyMessage());

        assertThat(m1.messageId, is(notNullValue()));
        assertThat(m1.messageId, is(not(equalTo(m2.messageId))));
    }

    @Test
    public void origin_messages_have_unique_correlation_IDs() {
        Envelope<DummyMessage> m1 = Envelope.newMessage(new DummyMessage());
        Envelope<DummyMessage> m2 = Envelope.newMessage(new DummyMessage());

        assertThat(m1.correlationId, is(notNullValue()));
        assertThat(m1.correlationId, is(not(equalTo(m2.correlationId))));
    }

    @Test
    public void origin_messages_do_not_have_causation_IDs() {
        Envelope<DummyMessage> m1 = Envelope.newMessage(new DummyMessage());

        assertThat(m1.causationId, is(nullValue()));
    }

    @Test
    public void outcome_messages_have_the_same_correlation_ID_as_their_cause() {
        Envelope<DummyMessage> origin = Envelope.newMessage(new DummyMessage());
        Envelope.setContext(origin);
        Envelope<DummyMessage> m1 = Envelope.newMessage(new DummyMessage());
        Envelope<DummyMessage> m2 = Envelope.newMessage(new DummyMessage());

        assertThat(m1.correlationId, is(notNullValue()));
        assertThat(m1.correlationId, is(origin.correlationId));
        assertThat(m2.correlationId, is(origin.correlationId));
    }

    @Test
    public void outcome_messages_have_the_message_ID_of_their_cause_as_their_causation_ID() {
        Envelope<DummyMessage> origin = Envelope.newMessage(new DummyMessage());
        Envelope.setContext(origin);
        Envelope<DummyMessage> m1 = Envelope.newMessage(new DummyMessage());
        Envelope<DummyMessage> m2 = Envelope.newMessage(new DummyMessage());

        assertThat(m1.causationId, is(notNullValue()));
        assertThat(m1.causationId, is(origin.messageId));
        assertThat(m2.causationId, is(origin.messageId));
    }

    @Test
    public void after_resetting_the_context_only_origin_messages_are_created() {
        Envelope.setContext(Envelope.newMessage(new DummyMessage()));
        Envelope<DummyMessage> m1 = Envelope.newMessage(new DummyMessage());
        assertThat(m1.causationId, is(notNullValue()));

        Envelope.resetContext();

        Envelope<DummyMessage> m2 = Envelope.newMessage(new DummyMessage());
        assertThat(m2.causationId, is(nullValue()));
    }

    private class DummyMessage implements Message {
    }
}