// Copyright © 2016 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

import java.util.HashMap;
import java.util.Map;

public class MessageRouter<T extends Message> implements Handles<T> {

    private final Map<Class<?>, Handles<T>> handlers = new HashMap<>();

    public <U extends T> void register(Class<U> messageType, Handles<U> handler) {
        if (handlers.containsKey(messageType)) {
            throw new IllegalStateException("handler for " + messageType + " already registered");
        }
        handlers.put(messageType, (Handles<T>) handler);
    }

    @Override
    public void handle(T message) {
        Class<? extends Message> type = message.getClass();
        Handles<T> handler = handlers.get(type);
        if (handler == null) {
            throw new IllegalArgumentException("no handler for " + type);
        }
        handler.handle(message);
    }
}
