// Copyright © 2016 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AggregateRoot {

    private static final Map<Class<?>, Map<Class<?>, Method>> eventListenersByAggregate = new ConcurrentHashMap<>();
    private final Map<Class<?>, Method> eventListeners;
    private final List<Event> changes = new ArrayList<>();
    private int version = 0;

    public AggregateRoot() {
        eventListeners = eventListenersByAggregate.computeIfAbsent(getClass(), AggregateRoot::findEventListeners);
    }

    public abstract UUID getId();

    public final int getVersion() {
        return version;
    }

    public final List<Event> getUncommittedChanges() {
        return Collections.unmodifiableList(changes);
    }

    public final void markChangesAsCommitted() {
        changes.clear();
    }

    public final void loadFromHistory(Iterable<Event> history) {
        for (Event event : history) {
            applyChange(event, false);
        }
    }

    protected final void publish(Event event) {
        applyChange(event, true);
    }

    private void applyChange(Event event, boolean isNew) {
        Method eventListener = eventListeners.get(event.getClass());
        if (eventListener != null) {
            try {
                eventListener.invoke(this, event);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("failed to apply change: " + event, e);
            }
        }
        version++;
        if (isNew) {
            changes.add(event);
        }
    }

    private static Map<Class<?>, Method> findEventListeners(Class<?> aggregate) {
        Map<Class<?>, Method> eventListeners = new HashMap<>();
        for (Method method : aggregate.getDeclaredMethods()) {
            if (method.isAnnotationPresent(EventListener.class)) {
                if (method.getParameterCount() != 1) {
                    throw new AssertionError("expected method to take exactly one parameter: " + method);
                }
                Class<?> eventType = method.getParameterTypes()[0];
                if (!Event.class.isAssignableFrom(eventType)) {
                    throw new AssertionError("expected method to take an event parameter: " + method);
                }
                if (!Modifier.isPrivate(method.getModifiers())) {
                    throw new AssertionError("expected method to be private: " + method);
                }
                method.setAccessible(true);
                eventListeners.put(eventType, method);
            }
        }
        return Collections.unmodifiableMap(eventListeners);
    }
}
