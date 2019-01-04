// Copyright © 2016-2019 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.reservation.commands;

import fi.luontola.cqrshotel.framework.Commit;
import fi.luontola.cqrshotel.framework.Handler;
import fi.luontola.cqrshotel.reservation.ReservationRepo;

import java.time.Clock;

public class MakeReservationHandler implements Handler<MakeReservation, Commit> {

    private final ReservationRepo repo;
    private final Clock clock;

    public MakeReservationHandler(ReservationRepo repo, Clock clock) {
        this.repo = repo;
        this.clock = clock;
    }

    @Override
    public Commit handle(MakeReservation command) {
        var reservation = repo.getById(command.reservationId);
        var originalVersion = reservation.getVersion();
        reservation.updateContactInformation(command.name, command.email);
        reservation.makeReservation(command.arrival, command.departure, clock);
        return repo.save(reservation, originalVersion);
    }
}
