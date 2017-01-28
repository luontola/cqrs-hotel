// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.reservation.queries;

import fi.luontola.cqrshotel.framework.EventListener;
import fi.luontola.cqrshotel.framework.Projection;
import fi.luontola.cqrshotel.hotel.Hotel;
import fi.luontola.cqrshotel.reservation.commands.SearchForAccommodation;
import fi.luontola.cqrshotel.reservation.events.PriceOffered;
import org.javamoney.moneta.Money;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ReservationOfferView implements Projection {

    private final Map<LocalDate, PriceOffered> offersByDate = new ConcurrentHashMap<>();
    private final Clock clock;

    public ReservationOfferView(Clock clock) {
        this.clock = clock;
    }

    @EventListener
    public void apply(PriceOffered offer) {
        offersByDate.put(offer.date, offer);
    }

    public ReservationOffer query(SearchForAccommodation query) {
        ReservationOffer result = new ReservationOffer();
        result.reservationId = query.reservationId;
        result.startDate = query.startDate;
        result.endDate = query.endDate;

        Money totalPrice = Money.of(0, Hotel.CURRENCY);
        for (LocalDate date = query.startDate; date.isBefore(query.endDate); date = date.plusDays(1)) {
            PriceOffered offer = offersByDate.get(date);
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
}
