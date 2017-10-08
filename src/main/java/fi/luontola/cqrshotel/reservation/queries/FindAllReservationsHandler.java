// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.reservation.queries;

import fi.luontola.cqrshotel.framework.Handler;

public class FindAllReservationsHandler implements Handler<FindAllReservations, ReservationDto[]> {

    private final ReservationsView projection;

    public FindAllReservationsHandler(ReservationsView projection) {
        this.projection = projection;
    }

    @Override
    public ReservationDto[] handle(FindAllReservations query) {
        return projection.findAll().toArray(new ReservationDto[0]);
    }
}
