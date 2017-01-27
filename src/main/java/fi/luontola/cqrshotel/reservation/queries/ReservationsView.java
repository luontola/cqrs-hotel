// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.reservation.queries;

import fi.luontola.cqrshotel.framework.EventListener;
import fi.luontola.cqrshotel.framework.Projection;
import fi.luontola.cqrshotel.reservation.events.ContactInformationUpdated;
import fi.luontola.cqrshotel.reservation.events.ReservationInitiated;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ReservationsView implements Projection {

    private final ConcurrentMap<UUID, ReservationDto> reservationsById = new ConcurrentHashMap<>();

    public List<ReservationDto> findAll() {
        return new ArrayList<>(reservationsById.values());
    }

    @EventListener
    public void apply(ReservationInitiated event) {
        ReservationDto reservation = getReservation(event.reservationId);
        reservation.checkInTime = event.checkInTime;
        reservation.checkOutTime = event.checkOutTime;
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
