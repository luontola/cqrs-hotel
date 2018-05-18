// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

public class UpdateProjectionsAfterHandling<M extends Message, R> implements Handler<M, R> {

    private final WorkersPool projections;
    private final Handler<M, R> handler;

    public UpdateProjectionsAfterHandling(WorkersPool projections, Handler<M, R> handler) {
        this.projections = projections;
        this.handler = handler;
    }

    @Override
    public R handle(M message) {
        R result = handler.handle(message);
        projections.updateAll();
        return result;
    }
}
