// Copyright © 2016 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public abstract class AggregateRoot {

    private final List<Event> changes = new ArrayList<>();
    private int version = 0;

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
        try {
            Method apply = getClass().getDeclaredMethod("apply", event.getClass());
            if (!Modifier.isPrivate(apply.getModifiers())) {
                throw new AssertionError("expected method to be private: " + apply);
            }
            apply.setAccessible(true);
            apply.invoke(this, event);
        } catch (NoSuchMethodException e) {
            // ignore; aggregate isn't interested in the event
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        version++;
        if (isNew) {
            changes.add(event);
        }
    }
}
