// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.reservation.queries;

import fi.luontola.cqrshotel.framework.Handler;

public class FindReservationByIdHandler implements Handler<FindReservationById, ReservationDto> {

    private final ReservationsView projection;

    public FindReservationByIdHandler(ReservationsView projection) {
        this.projection = projection;
    }

    @Override
    public ReservationDto handle(FindReservationById query) {
        return projection.findById(query.reservationId);
    }
}
