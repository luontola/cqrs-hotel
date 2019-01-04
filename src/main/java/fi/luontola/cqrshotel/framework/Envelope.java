// Copyright © 2016-2019 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

import fi.luontola.cqrshotel.framework.util.Struct;
import fi.luontola.cqrshotel.framework.util.UUIDs;

import java.util.UUID;

public class Envelope<M extends Message> extends Struct {

    private static final ThreadLocal<Envelope<?>> threadContext = new ThreadLocal<>();

    public final M payload;
    public final UUID messageId;
    public final UUID correlationId;
    public final UUID causationId;

    public static <M extends Message> Envelope<M> newMessage(M payload) {
        var cause = getContext();
        if (cause == null) {
            return new Envelope<>(payload, UUIDs.newUUID(), UUIDs.newUUID(), null);
        } else {
            return newMessage(payload, cause);
        }
    }

    public static <M extends Message> Envelope<M> newMessage(M payload, Envelope<?> cause) {
        return new Envelope<>(payload, UUIDs.newUUID(), cause.correlationId, cause.messageId);
    }

    private static Envelope<?> getContext() {
        return threadContext.get();
    }

    public static void setContext(Envelope<?> context) {
        threadContext.set(context);
    }

    public static void resetContext() {
        threadContext.remove();
    }

    public Envelope(M payload, UUID messageId, UUID correlationId, UUID causationId) {
        this.messageId = messageId;
        this.correlationId = correlationId;
        this.causationId = causationId;
        this.payload = payload;
    }

    public Envelope<M> withCorrelationId(UUID newCorrelationId) {
        return new Envelope<>(payload, messageId, newCorrelationId, causationId);
    }

    public String metaToString() {
        return "messageId=" + messageId + ", correlationId=" + correlationId + ", causationId=" + causationId;
    }
}
