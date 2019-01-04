// Copyright © 2016-2019 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.reservation.queries;

import fi.luontola.cqrshotel.framework.projections.AnnotatedProjection;
import fi.luontola.cqrshotel.framework.util.EventListener;
import fi.luontola.cqrshotel.reservation.events.ContactInformationUpdated;
import fi.luontola.cqrshotel.reservation.events.ReservationCreated;
import fi.luontola.cqrshotel.reservation.events.RoomAssigned;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ReservationsView extends AnnotatedProjection {

    public static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("d.M.yyyy HH:mm");

    private final ConcurrentMap<UUID, ReservationDto> reservationsById = new ConcurrentHashMap<>();

    @EventListener
    public void apply(ReservationCreated event) {
        var reservation = getReservation(event.reservationId);
        reservation.arrival = event.arrival;
        reservation.departure = event.departure;
        reservation.checkInTime = event.checkInTime.format(DATE_TIME_FORMAT);
        reservation.checkOutTime = event.checkOutTime.format(DATE_TIME_FORMAT);
        reservation.status = "initiated";
    }

    @EventListener
    public void apply(ContactInformationUpdated event) {
        var reservation = getReservation(event.reservationId);
        reservation.name = event.name;
        reservation.email = event.email;
    }

    @EventListener
    public void apply(RoomAssigned event) {
        var reservation = getReservation(event.reservationId);
        reservation.roomId = event.roomId;
        reservation.roomNumber = event.roomNumber;
    }

    // queries

    public List<ReservationDto> findAll() {
        return new ArrayList<>(reservationsById.values());
    }

    public ReservationDto findById(UUID reservationId) {
        return reservationsById.get(reservationId);
    }

    // helpers

    private ReservationDto getReservation(UUID reservationId) {
        return reservationsById.computeIfAbsent(reservationId, id -> {
            var r = new ReservationDto();
            r.reservationId = id;
            return r;
        });
    }
}
