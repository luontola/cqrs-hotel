// Copyright © 2016 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

import java.util.HashMap;
import java.util.Map;

public class CompositeHandler<M extends Message, R> implements Handler<M, R> {

    private final Map<Class<?>, Handler<M, R>> handlers = new HashMap<>();

    public <U extends M> void register(Class<U> type, Handler<U, ?> handler) {
        if (handlers.containsKey(type)) {
            throw new IllegalStateException("handler for " + type + " already registered");
        }
        handlers.put(type, (Handler<M, R>) handler);
    }

    @Override
    public R handle(M query) {
        Class<?> type = query.getClass();
        Handler<M, R> handler = handlers.get(type);
        if (handler == null) {
            throw new IllegalArgumentException("no handler for " + type);
        }
        return handler.handle(query);
    }
}
