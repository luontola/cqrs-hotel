// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework.consistency;

import fi.luontola.cqrshotel.framework.Command;
import fi.luontola.cqrshotel.framework.Commit;
import fi.luontola.cqrshotel.framework.Handler;

public class UpdateObservedPositionAfterCommit implements Handler<Command, Commit> {

    private final ObservedPosition observedPosition;
    private final Handler<Command, Commit> handler;

    public UpdateObservedPositionAfterCommit(ObservedPosition observedPosition, Handler<Command, Commit> handler) {
        this.observedPosition = observedPosition;
        this.handler = handler;
    }

    @Override
    public Commit handle(Command message) {
        Commit commit = handler.handle(message);
        observedPosition.observe(commit.committedPosition);
        return commit;
    }
}
