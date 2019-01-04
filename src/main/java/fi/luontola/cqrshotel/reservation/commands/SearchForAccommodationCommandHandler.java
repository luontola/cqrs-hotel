// Copyright © 2016-2019 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.reservation.commands;

import fi.luontola.cqrshotel.framework.Commit;
import fi.luontola.cqrshotel.framework.Handler;
import fi.luontola.cqrshotel.pricing.PricingEngine;
import fi.luontola.cqrshotel.reservation.Reservation;
import fi.luontola.cqrshotel.reservation.ReservationRepo;

import java.time.Clock;

public class SearchForAccommodationCommandHandler implements Handler<SearchForAccommodation, Commit> {

    private final ReservationRepo repo;
    private final PricingEngine pricing;
    private final Clock clock;

    public SearchForAccommodationCommandHandler(ReservationRepo repo, PricingEngine pricing, Clock clock) {
        this.repo = repo;
        this.pricing = pricing;
        this.clock = clock;
    }

    @Override
    public Commit handle(SearchForAccommodation command) {
        Reservation reservation = repo.createOrGet(command.reservationId);
        int originalVersion = reservation.getVersion();
        reservation.searchForAccommodation(command.arrival, command.departure, pricing, clock);
        return repo.save(reservation, originalVersion);
    }
}
