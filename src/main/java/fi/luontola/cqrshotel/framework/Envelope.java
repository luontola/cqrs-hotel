// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

import fi.luontola.cqrshotel.util.Struct;

import java.util.UUID;

public class Envelope<M extends Message> extends Struct {

    private static final ThreadLocal<Envelope<?>> threadContext = new ThreadLocal<>();

    public final UUID messageId;
    public final UUID correlationId;
    public final UUID causationId;
    public final M payload;

    public static <M extends Message> Envelope<M> newMessage(M payload) {
        Envelope<?> cause = threadContext.get();
        if (cause == null) {
            return new Envelope<>(UUIDs.newUUID(), UUIDs.newUUID(), null, payload);
        } else {
            return new Envelope<>(UUIDs.newUUID(), cause.correlationId, cause.messageId, payload);
        }
    }

    public static void setContext(Envelope<?> context) {
        threadContext.set(context);
    }

    public static void resetContext() {
        threadContext.remove();
    }

    public Envelope(UUID messageId, UUID correlationId, UUID causationId, M payload) {
        this.messageId = messageId;
        this.correlationId = correlationId;
        this.causationId = causationId;
        this.payload = payload;
    }
}
