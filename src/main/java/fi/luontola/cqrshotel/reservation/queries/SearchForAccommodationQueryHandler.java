// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.reservation.queries;

import fi.luontola.cqrshotel.framework.EventStore;
import fi.luontola.cqrshotel.framework.Handler;
import fi.luontola.cqrshotel.framework.InMemorySingleStreamProjectionUpdater;
import fi.luontola.cqrshotel.reservation.commands.SearchForAccommodation;

import java.time.Clock;

public class SearchForAccommodationQueryHandler implements Handler<SearchForAccommodation, ReservationOffer> {

    private final EventStore eventStore;
    private final Clock clock;

    public SearchForAccommodationQueryHandler(EventStore eventStore, Clock clock) {
        this.eventStore = eventStore;
        this.clock = clock;
    }

    @Override
    public ReservationOffer handle(SearchForAccommodation command) {
        ReservationOfferView projection = new ReservationOfferView(clock);
        new InMemorySingleStreamProjectionUpdater(command.reservationId, projection, eventStore).update();
        return projection.query(command);
    }
}
