// Copyright © 2016 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.reservation.queries;

import fi.luontola.cqrshotel.framework.Event;
import fi.luontola.cqrshotel.framework.EventStore;
import fi.luontola.cqrshotel.framework.Queries;
import fi.luontola.cqrshotel.reservation.commands.SearchForAccommodation;
import fi.luontola.cqrshotel.reservation.events.PriceOffered;
import org.javamoney.moneta.Money;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toMap;

public class SearchForAccommodationQuery implements Queries<SearchForAccommodation, ReservationOffer> {

    private final EventStore eventStore;
    private final Clock clock;

    public SearchForAccommodationQuery(EventStore eventStore, Clock clock) {
        this.eventStore = eventStore;
        this.clock = clock;
    }

    @Override
    public ReservationOffer query(SearchForAccommodation command) {
        List<Event> events = eventStore.getEventsForStream(command.reservationId);
        Map<LocalDate, PriceOffered> offersByDate = events.stream()
                .filter(e -> e instanceof PriceOffered)
                .map(e -> (PriceOffered) e)
                .filter(offer -> offer.isInRange(command.startDate, command.endDate))
                .filter(offer -> offer.isStillValid(clock))
                .collect(toMap(offer -> offer.date, offer -> offer));

        Set<LocalDate> datesInRange = new HashSet<>();
        for (LocalDate date = command.startDate; date.isBefore(command.endDate); date = date.plusDays(1)) {
            datesInRange.add(date);
        }

        Money totalPrice;
        if (datesInRange.equals(offersByDate.keySet())) {
            totalPrice = offersByDate.values().stream()
                    .map(offer -> offer.price)
                    .reduce(Money::add)
                    .get();
        } else {
            totalPrice = null;
        }

        ReservationOffer result = new ReservationOffer();
        result.reservationId = command.reservationId;
        result.startDate = command.startDate;
        result.endDate = command.endDate;
        result.totalPrice = totalPrice;
        return result;
    }
}
