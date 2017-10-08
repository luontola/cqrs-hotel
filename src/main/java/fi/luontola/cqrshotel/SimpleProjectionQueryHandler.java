// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel;

import fi.luontola.cqrshotel.framework.Handler;
import fi.luontola.cqrshotel.framework.Projection;
import fi.luontola.cqrshotel.framework.consistency.ObservedPosition;

public class SimpleProjectionQueryHandler<P extends Projection> implements Handler<SimpleProjectionQuery<P>, Object> {

    private final ObservedPosition observedPosition;

    public SimpleProjectionQueryHandler(ObservedPosition observedPosition) {
        this.observedPosition = observedPosition;
    }

    @Override
    public Object handle(SimpleProjectionQuery<P> query) {
        P projection = query.projection;
        observedPosition.waitForProjectionToUpdate(projection);
        Object result = query.method.apply(projection);
        observedPosition.observe(projection.getPosition());
        return result;
    }
}
