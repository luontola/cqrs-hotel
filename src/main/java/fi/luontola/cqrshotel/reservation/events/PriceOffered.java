// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.reservation.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import fi.luontola.cqrshotel.framework.Event;
import fi.luontola.cqrshotel.util.Struct;
import org.javamoney.moneta.Money;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class PriceOffered extends Struct implements Event {

    public final UUID reservationId;
    public final LocalDate date;
    public final Money price;
    public final Instant expires;

    @JsonCreator
    public PriceOffered(@JsonProperty("reservationId") UUID reservationId,
                        @JsonProperty("date") LocalDate date,
                        @JsonProperty("price") Money price,
                        @JsonProperty("expires") Instant expires) {
        this.reservationId = reservationId;
        this.date = date;
        this.price = price;
        this.expires = expires;
    }

    public boolean isInRange(LocalDate startDate, LocalDate endDate) {
        return (date.equals(startDate) || date.isAfter(startDate))
                && date.isBefore(endDate);
    }

    public boolean hasExpired(Clock clock) {
        return !isStillValid(clock);
    }

    public boolean isStillValid(Clock clock) {
        return expires.isAfter(clock.instant());
    }
}
