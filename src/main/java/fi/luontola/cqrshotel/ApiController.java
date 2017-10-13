// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel;

import fi.luontola.cqrshotel.capacity.queries.CapacityDto;
import fi.luontola.cqrshotel.capacity.queries.GetCapacityByDate;
import fi.luontola.cqrshotel.capacity.queries.GetCapacityByDateRange;
import fi.luontola.cqrshotel.reservation.commands.MakeReservation;
import fi.luontola.cqrshotel.reservation.commands.SearchForAccommodation;
import fi.luontola.cqrshotel.reservation.queries.FindAllReservations;
import fi.luontola.cqrshotel.reservation.queries.FindReservationById;
import fi.luontola.cqrshotel.reservation.queries.ReservationDto;
import fi.luontola.cqrshotel.reservation.queries.ReservationOffer;
import fi.luontola.cqrshotel.room.commands.CreateRoom;
import fi.luontola.cqrshotel.room.queries.FindAllRooms;
import fi.luontola.cqrshotel.room.queries.RoomDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
public class ApiController {

    private final Core core;

    public ApiController(Core core) {
        this.core = core;
    }

    @GetMapping("/api")
    public String home() {
        return "CQRS Hotel API";
    }

    @GetMapping("/api/status")
    public StatusPage status() {
        return core.getStatus();
    }

    @PostMapping("/api/search-for-accommodation")
    public ReservationOffer searchForAccommodation(@RequestBody SearchForAccommodation command) {
        return (ReservationOffer) core.handle(command);
    }

    @PostMapping("/api/make-reservation")
    public Map<?, ?> makeReservation(@RequestBody MakeReservation command) {
        return (Map<?, ?>) core.handle(command); // XXX: WriteObservedPositionToResponseHeaders works only for non-void controller methods
    }

    @GetMapping("/api/reservations")
    public ReservationDto[] reservations() {
        return (ReservationDto[]) core.handle(new FindAllReservations());
    }

    @GetMapping("/api/reservations/{reservationId}")
    public ReservationDto reservationById(@PathVariable String reservationId) {
        return (ReservationDto) core.handle(new FindReservationById(UUID.fromString(reservationId)));
    }

    @PostMapping("/api/create-room")
    public Map<?, ?> createRoom(@RequestBody CreateRoom command) {
        return (Map<?, ?>) core.handle(command); // XXX: WriteObservedPositionToResponseHeaders works only for non-void controller methods
    }

    @GetMapping("/api/rooms")
    public RoomDto[] rooms() {
        return (RoomDto[]) core.handle(new FindAllRooms());
    }

    @GetMapping("/api/capacity/{date}")
    public CapacityDto capacityByDate(@PathVariable String date) {
        return (CapacityDto) core.handle(new GetCapacityByDate(LocalDate.parse(date)));
    }

    @GetMapping("/api/capacity/{start}/{end}")
    public CapacityDto[] capacityByDateRange(@PathVariable String start,
                                             @PathVariable String end) {
        return (CapacityDto[]) core.handle(new GetCapacityByDateRange(LocalDate.parse(start), LocalDate.parse(end)));
    }
}
