// Copyright © 2016-2019 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework.consistency;

import fi.luontola.cqrshotel.framework.Handler;
import fi.luontola.cqrshotel.framework.Message;
import fi.luontola.cqrshotel.framework.projections.UpdatableProjection;

public class WaitForProjectionToUpdate<M extends Message, R> implements Handler<M, R> {

    private final UpdatableProjection projection;
    private final ObservedPosition observedPosition;
    private final Handler<M, R> handler;

    public WaitForProjectionToUpdate(UpdatableProjection projection, ObservedPosition observedPosition, Handler<M, R> handler) {
        this.projection = projection;
        this.observedPosition = observedPosition;
        this.handler = handler;
    }

    @Override
    public R handle(M message) {
        observedPosition.waitForProjectionToUpdate(projection);
        var result = handler.handle(message);
        observedPosition.observe(projection.getPosition());
        return result;
    }
}
