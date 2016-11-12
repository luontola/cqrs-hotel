// Copyright © 2016 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.reservation.commands;

import fi.luontola.cqrshotel.framework.Handles;
import fi.luontola.cqrshotel.reservation.Reservation;
import fi.luontola.cqrshotel.reservation.ReservationRepo;

public class MakeReservationHandler implements Handles<MakeReservation> {

    private final ReservationRepo repo;

    public MakeReservationHandler(ReservationRepo repo) {
        this.repo = repo;
    }

    @Override
    public void handle(MakeReservation command) {
        Reservation reservation = repo.getById(command.reservationId);
        int originalVersion = reservation.getVersion();
        reservation.updateContactInformation(command.name, command.email);
        reservation.makeReservation(command.startDate, command.endDate);
        repo.save(reservation, originalVersion);
    }
}
