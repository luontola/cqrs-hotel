// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework.processes;

import fi.luontola.cqrshotel.framework.Envelope;
import fi.luontola.cqrshotel.framework.Event;
import fi.luontola.cqrshotel.framework.Projection;
import fi.luontola.cqrshotel.framework.Publisher;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class PersistedProcess {

    private final Class<?> processType;
    public final List<Envelope<Event>> history = new ArrayList<>();

    public PersistedProcess(Class<?> processType) {
        this.processType = processType;
    }

    public Projection newInstance(Publisher publisher) {
        try {
            return (Projection) processType.getConstructor(Publisher.class).newInstance(publisher);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw new RuntimeException("Failed to instantiate " + processType, e);
        }
    }
}
