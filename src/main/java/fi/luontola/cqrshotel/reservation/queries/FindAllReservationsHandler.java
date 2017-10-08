// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.reservation.queries;

import fi.luontola.cqrshotel.framework.Handler;
import fi.luontola.cqrshotel.framework.consistency.ObservedPosition;

import java.util.List;

public class FindAllReservationsHandler implements Handler<FindAllReservations, List<ReservationDto>> {

    private final ReservationsView projection;
    private final ObservedPosition observedPosition;

    public FindAllReservationsHandler(ReservationsView projection, ObservedPosition observedPosition) {
        this.projection = projection;
        this.observedPosition = observedPosition;
    }

    @Override
    public List<ReservationDto> handle(FindAllReservations query) {
        observedPosition.waitForProjectionToUpdate(projection);
        return projection.findAll();
    }
}
