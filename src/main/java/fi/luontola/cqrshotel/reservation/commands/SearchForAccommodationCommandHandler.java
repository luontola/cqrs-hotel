// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.reservation.commands;

import fi.luontola.cqrshotel.framework.EventStreamNotFoundException;
import fi.luontola.cqrshotel.framework.Handler;
import fi.luontola.cqrshotel.pricing.PricingEngine;
import fi.luontola.cqrshotel.reservation.Reservation;
import fi.luontola.cqrshotel.reservation.ReservationRepo;

import java.time.Clock;

public class SearchForAccommodationCommandHandler implements Handler<SearchForAccommodation, Void> {

    private final ReservationRepo repo;
    private final PricingEngine pricing;
    private final Clock clock;

    public SearchForAccommodationCommandHandler(ReservationRepo repo, PricingEngine pricing, Clock clock) {
        this.repo = repo;
        this.pricing = pricing;
        this.clock = clock;
    }

    @Override
    public Void handle(SearchForAccommodation command) {
        Reservation reservation;
        int originalVersion;
        try {
            reservation = repo.getById(command.reservationId);
            originalVersion = reservation.getVersion();
        } catch (EventStreamNotFoundException e) {
            reservation = repo.create(command.reservationId);
            originalVersion = reservation.getVersion();
            reservation.discoverCustomer();
        }
        reservation.searchForAccommodation(command.startDate, command.endDate, pricing, clock);
        repo.save(reservation, originalVersion);
        return null;
    }
}
