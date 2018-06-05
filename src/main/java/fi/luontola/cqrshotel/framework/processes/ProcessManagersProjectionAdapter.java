// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework.processes;

import fi.luontola.cqrshotel.framework.Envelope;
import fi.luontola.cqrshotel.framework.Event;
import fi.luontola.cqrshotel.framework.projections.Projection;

public class ProcessManagersProjectionAdapter implements Projection {
    // TODO: remove this class; ProcessManagers needs a smarter projection which keeps track of the processes individually

    private final ProcessManagers processManagers;

    public ProcessManagersProjectionAdapter(ProcessManagers processManagers) {
        this.processManagers = processManagers;
    }

    @Override
    public void apply(Envelope<Event> event) {
        processManagers.handle(event);
    }
}
