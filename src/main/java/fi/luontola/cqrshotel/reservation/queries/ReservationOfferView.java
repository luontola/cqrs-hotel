// Copyright © 2016-2019 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.reservation.queries;

import fi.luontola.cqrshotel.framework.projections.AnnotatedProjection;
import fi.luontola.cqrshotel.framework.util.EventListener;
import fi.luontola.cqrshotel.hotel.Hotel;
import fi.luontola.cqrshotel.reservation.commands.SearchForAccommodation;
import fi.luontola.cqrshotel.reservation.events.PriceOffered;
import org.javamoney.moneta.Money;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ReservationOfferView extends AnnotatedProjection {

    private final Map<LocalDate, PriceOffered> offersByDate = new ConcurrentHashMap<>();
    private final UUID reservationId;
    private final Clock clock;

    public ReservationOfferView(UUID reservationId, Clock clock) {
        this.reservationId = reservationId;
        this.clock = clock;
    }

    @EventListener
    public void apply(PriceOffered offer) {
        checkReservationId(offer.reservationId);
        offersByDate.put(offer.date, offer);
    }

    // queries

    public ReservationOffer query(SearchForAccommodation query) {
        checkReservationId(query.reservationId);
        var result = new ReservationOffer();
        result.reservationId = query.reservationId;
        result.arrival = query.arrival;
        result.departure = query.departure;

        var totalPrice = Money.of(0, Hotel.CURRENCY);
        for (var date = query.arrival; date.isBefore(query.departure); date = date.plusDays(1)) {
            var offer = offersByDate.get(date);
            if (offer != null && offer.isStillValid(clock)) {
                totalPrice = totalPrice.add(offer.price);
            } else {
                // offer missing for some day; don't set result.totalPrice
                return result;
            }
        }

        result.totalPrice = totalPrice;
        return result;
    }

    // helpers

    private void checkReservationId(UUID reservationId) {
        if (!this.reservationId.equals(reservationId)) {
            throw new IllegalArgumentException("this projection instance works only for a single reservation");
        }
    }
}
