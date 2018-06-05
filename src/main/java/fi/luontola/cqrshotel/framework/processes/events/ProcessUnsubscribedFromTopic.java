// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework.processes.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import fi.luontola.cqrshotel.framework.Event;
import fi.luontola.cqrshotel.framework.util.Struct;

import java.util.UUID;

public class ProcessUnsubscribedFromTopic extends Struct implements Event {

    public final UUID processId;
    public final UUID topic;

    @JsonCreator
    public ProcessUnsubscribedFromTopic(@JsonProperty("processId") UUID processId,
                                        @JsonProperty("topic") UUID topic) {
        this.processId = processId;
        this.topic = topic;
    }
}
