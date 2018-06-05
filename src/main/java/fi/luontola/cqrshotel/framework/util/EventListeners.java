// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework.util;

import fi.luontola.cqrshotel.framework.Event;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EventListeners {

    private static final Map<Class<?>, Map<Class<?>, Method>> eventListenersCache = new ConcurrentHashMap<>();
    private final Map<Class<?>, Method> eventListeners;
    private final Object target;

    public static EventListeners of(Object target, Requirements... requirements) {
        return new EventListeners(target, requirements);
    }

    private EventListeners(Object target, Requirements[] requirements) {
        // XXX: requirements are checked only when adding to cache, so subsequent calls with different requirements on the same type will bypass the checks
        this.eventListeners = eventListenersCache.computeIfAbsent(target.getClass(), targetType -> findEventListeners(targetType, requirements));
        this.target = target;
    }

    private static Map<Class<?>, Method> findEventListeners(Class<?> targetType, Requirements[] requirements) {
        Map<Class<?>, Method> eventListeners = new HashMap<>();
        for (Method method : targetType.getDeclaredMethods()) {
            if (method.isAnnotationPresent(EventListener.class)) {
                if (method.getParameterCount() != 1) {
                    throw new IllegalArgumentException("expected method to take exactly one parameter: " + method);
                }
                Class<?> eventType = method.getParameterTypes()[0];
                if (!Event.class.isAssignableFrom(eventType)) {
                    throw new IllegalArgumentException("expected method to take an event parameter: " + method);
                }
                for (Requirements requirement : requirements) {
                    requirement.check(method);
                }
                method.setAccessible(true);
                eventListeners.put(eventType, method);
            }
        }
        return Collections.unmodifiableMap(eventListeners);
    }

    public void send(Event event) {
        Method method = eventListeners.get(event.getClass());
        if (method != null) {
            try {
                method.invoke(target, event);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("event listener failed for event: " + event, e);
            }
        }
    }

    public enum Requirements {

        MUST_BE_PRIVATE {
            public void check(Method method) {
                if (!Modifier.isPrivate(method.getModifiers())) {
                    throw new IllegalArgumentException("expected method to be private: " + method);
                }
            }
        };

        public abstract void check(Method method);
    }
}
