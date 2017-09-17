// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.reservation.commands;

import fi.luontola.cqrshotel.framework.Handler;
import fi.luontola.cqrshotel.reservation.Reservation;
import fi.luontola.cqrshotel.reservation.ReservationRepo;

import java.time.Clock;

public class MakeReservationHandler implements Handler<MakeReservation, Void> {

    private final ReservationRepo repo;
    private final Clock clock;

    public MakeReservationHandler(ReservationRepo repo, Clock clock) {
        this.repo = repo;
        this.clock = clock;
    }

    @Override
    public Void handle(MakeReservation command) {
        Reservation reservation = repo.getById(command.reservationId);
        int originalVersion = reservation.getVersion();
        reservation.updateContactInformation(command.name, command.email);
        reservation.makeReservation(command.arrival, command.departure, clock);
        repo.save(reservation, originalVersion);
        return null;
    }
}
