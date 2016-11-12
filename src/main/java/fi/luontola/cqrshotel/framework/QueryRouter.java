// Copyright © 2016 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

import java.util.HashMap;
import java.util.Map;

public class QueryRouter<T extends Query> implements Queries<T, Object> {

    private final Map<Class<?>, Queries<T, Object>> handlers = new HashMap<>();

    public <U extends T> void register(Class<U> type, Queries<U, ?> handler) {
        if (handlers.containsKey(type)) {
            throw new IllegalStateException("handler for " + type + " already registered");
        }
        handlers.put(type, (Queries<T, Object>) handler);
    }

    @Override
    public Object query(T query) {
        Class<?> type = query.getClass();
        Queries<T, Object> handler = handlers.get(type);
        if (handler == null) {
            throw new IllegalArgumentException("no handler for " + type);
        }
        return handler.query(query);
    }
}
