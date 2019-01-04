// Copyright © 2016-2019 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SpyMessageGateway implements MessageGateway {

    public final List<Envelope<?>> outgoing = new ArrayList<>();

    public List<Message> outgoingMessages() {
        return outgoing.stream()
                .map(m -> m.payload)
                .collect(Collectors.toList());
    }

    @Override
    public void send(Envelope<?> message) {
        outgoing.add(message);
    }

    public Envelope<?> latestMessage() {
        var size = outgoing.size();
        if (size == 0) {
            throw new IllegalStateException("no messages");
        }
        return outgoing.get(size - 1);
    }
}
