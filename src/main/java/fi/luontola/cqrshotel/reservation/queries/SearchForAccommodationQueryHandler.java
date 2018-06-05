// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.reservation.queries;

import fi.luontola.cqrshotel.framework.Envelope;
import fi.luontola.cqrshotel.framework.Event;
import fi.luontola.cqrshotel.framework.Handler;
import fi.luontola.cqrshotel.framework.eventstore.EventStore;
import fi.luontola.cqrshotel.framework.projections.Projection;
import fi.luontola.cqrshotel.reservation.commands.SearchForAccommodation;

import java.time.Clock;
import java.util.UUID;

public class SearchForAccommodationQueryHandler implements Handler<SearchForAccommodation, ReservationOffer> {

    private final EventStore eventStore;
    private final Clock clock;

    public SearchForAccommodationQueryHandler(EventStore eventStore, Clock clock) {
        this.eventStore = eventStore;
        this.clock = clock;
    }

    @Override
    public ReservationOffer handle(SearchForAccommodation query) {
        UUID reservationId = query.reservationId;
        ReservationOfferView view = new ReservationOfferView(reservationId, clock);
        applyEventsFromStream(reservationId, view);
        return view.query(query);
    }

    private void applyEventsFromStream(UUID streamId, Projection projection) {
        for (Envelope<Event> event : eventStore.getEventsForStream(streamId, EventStore.BEGINNING)) {
            projection.apply(event);
        }
    }
}
