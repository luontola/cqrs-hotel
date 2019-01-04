// Copyright © 2016-2019 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.capacity.queries;

import fi.luontola.cqrshotel.framework.projections.AnnotatedProjection;
import fi.luontola.cqrshotel.framework.util.EventListener;
import fi.luontola.cqrshotel.reservation.events.ReservationCreated;
import fi.luontola.cqrshotel.room.events.RoomCreated;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class CapacityView extends AnnotatedProjection {

    private final AtomicInteger numberOfRooms = new AtomicInteger(0);
    private final ConcurrentMap<LocalDate, AtomicInteger> reservationsByDate = new ConcurrentHashMap<>();

    @EventListener
    public void apply(RoomCreated event) {
        numberOfRooms.incrementAndGet();
    }

    @EventListener
    public void apply(ReservationCreated event) {
        for (var date = event.arrival; date.isBefore(event.departure); date = date.plusDays(1)) {
            reservationsByDate.computeIfAbsent(date, _date -> new AtomicInteger(0))
                    .incrementAndGet();
        }
    }

    // queries

    public CapacityDto getCapacityByDate(LocalDate date) {
        var capacity = new CapacityDto();
        capacity.date = date;
        capacity.capacity = numberOfRooms.intValue();
        capacity.reserved = reservationsByDate.getOrDefault(date, new AtomicInteger(0)).intValue();
        return capacity;
    }

    public List<CapacityDto> getCapacityByDateRange(LocalDate start, LocalDate endInclusive) {
        var endExclusive = endInclusive.plusDays(1);
        List<CapacityDto> results = new ArrayList<>();
        for (var date = start; date.isBefore(endExclusive); date = date.plusDays(1)) {
            results.add(getCapacityByDate(date));
        }
        return results;
    }
}
