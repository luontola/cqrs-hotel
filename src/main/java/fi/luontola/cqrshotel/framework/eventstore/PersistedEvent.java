// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework.eventstore;

import fi.luontola.cqrshotel.framework.Envelope;
import fi.luontola.cqrshotel.framework.Event;
import fi.luontola.cqrshotel.framework.util.Struct;

import java.util.UUID;

public class PersistedEvent extends Struct {

    public final Envelope<Event> event;
    public final UUID streamId;
    public final int version;
    public final long position;

    public PersistedEvent(Envelope<Event> event, UUID streamId, int version, long position) {
        this.event = event;
        this.streamId = streamId;
        this.version = version;
        this.position = position;
    }
}
