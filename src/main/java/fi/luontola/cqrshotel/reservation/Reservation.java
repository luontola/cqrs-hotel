// Copyright © 2016 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.reservation;

import fi.luontola.cqrshotel.framework.AggregateRoot;
import fi.luontola.cqrshotel.reservation.events.ContactInformationUpdated;
import fi.luontola.cqrshotel.reservation.events.ReservationInitialized;
import fi.luontola.cqrshotel.reservation.events.ReservationMade;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;

public class Reservation extends AggregateRoot {

    public static final ZoneId TIMEZONE = ZoneId.systemDefault();
    public static final LocalTime CHECK_IN_TIME = LocalTime.of(14, 0);
    public static final LocalTime CHECK_OUT_TIME = LocalTime.of(12, 0);

    private UUID id;

    @Override
    public UUID getId() {
        return id;
    }

    private void apply(ReservationInitialized event) {
        id = event.reservationId;
    }

    public void updateContactInformation(String name, String email) {
        publish(new ContactInformationUpdated(id, name, email));
    }

    public void makeReservation(LocalDate startDate, LocalDate endDate) {
        Instant checkInTime = startDate
                .atTime(CHECK_IN_TIME)
                .atZone(TIMEZONE)
                .toInstant();
        Instant checkOutTime = endDate
                .atTime(CHECK_OUT_TIME)
                .atZone(TIMEZONE)
                .toInstant();
        publish(new ReservationMade(id, checkInTime, checkOutTime));
    }
}
