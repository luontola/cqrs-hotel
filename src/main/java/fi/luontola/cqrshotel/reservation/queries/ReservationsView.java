// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.reservation.queries;

import fi.luontola.cqrshotel.framework.EventListener;
import fi.luontola.cqrshotel.framework.Projection;
import fi.luontola.cqrshotel.reservation.events.ContactInformationUpdated;
import fi.luontola.cqrshotel.reservation.events.ReservationInitiated;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ReservationsView implements Projection {

    public static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("d.M.yyyy HH:mm");

    private final ConcurrentMap<UUID, ReservationDto> reservationsById = new ConcurrentHashMap<>();

    public List<ReservationDto> findAll() {
        return new ArrayList<>(reservationsById.values());
    }

    public ReservationDto findById(UUID reservationId) {
        return reservationsById.get(reservationId);
    }

    @EventListener
    public void apply(ReservationInitiated event) {
        ReservationDto reservation = getReservation(event.reservationId);
        reservation.arrival = event.arrival;
        reservation.departure = event.departure;
        reservation.checkInTime = event.checkInTime.format(DATE_TIME_FORMAT);
        reservation.checkOutTime = event.checkOutTime.format(DATE_TIME_FORMAT);
        reservation.status = "initiated";
    }

    @EventListener
    public void apply(ContactInformationUpdated event) {
        ReservationDto reservation = getReservation(event.reservationId);
        reservation.name = event.name;
        reservation.email = event.email;
    }

    private ReservationDto getReservation(UUID reservationId) {
        return reservationsById.computeIfAbsent(reservationId, id -> {
            ReservationDto r = new ReservationDto();
            r.reservationId = id;
            return r;
        });
    }
}
