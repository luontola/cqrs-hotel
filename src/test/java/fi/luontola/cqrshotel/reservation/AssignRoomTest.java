// Copyright © 2016-2019 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.reservation;

import fi.luontola.cqrshotel.framework.AggregateRootTester;
import fi.luontola.cqrshotel.hotel.Hotel;
import fi.luontola.cqrshotel.reservation.commands.AssignRoom;
import fi.luontola.cqrshotel.reservation.commands.AssignRoomHandler;
import fi.luontola.cqrshotel.reservation.events.ReservationCreated;
import fi.luontola.cqrshotel.reservation.events.RoomAssigned;
import fi.luontola.cqrshotel.reservation.events.SearchedForAccommodation;
import fi.luontola.cqrshotel.room.events.RoomCreated;
import fi.luontola.cqrshotel.room.queries.GetRoomByIdHandler;
import fi.luontola.cqrshotel.room.queries.RoomsView;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("fast")
public class AssignRoomTest extends AggregateRootTester {

    private static final LocalDate arrival = LocalDate.of(2000, 1, 1);
    private static final LocalDate departure = LocalDate.of(2000, 1, 5);
    private static final UUID roomId = UUID.randomUUID();
    private static final UUID roomId2 = UUID.randomUUID();
    private static final String roomNumber = "101";
    private static final String roomNumber2 = "102";

    {
        var roomsView = new RoomsView();
        roomsView.apply(new RoomCreated(roomId, roomNumber));
        roomsView.apply(new RoomCreated(roomId2, roomNumber2));
        var getRoomById = new GetRoomByIdHandler(roomsView);
        commandHandler = new AssignRoomHandler(new ReservationRepo(eventStore), getRoomById);
    }

    @Test
    public void assigns_the_room_to_the_reservation() {
        given(new ReservationCreated(id, arrival, departure, Hotel.checkInTime(arrival), Hotel.checkOutTime(departure)));

        when(new AssignRoom(id, roomId));

        then(new RoomAssigned(id, roomId, roomNumber));
    }

    @Test
    public void the_assigned_room_can_be_changed() {
        given(new ReservationCreated(id, arrival, departure, Hotel.checkInTime(arrival), Hotel.checkOutTime(departure)),
                new RoomAssigned(id, roomId, roomNumber));

        when(new AssignRoom(id, roomId2));

        then(new RoomAssigned(id, roomId2, roomNumber2));
    }

    @Test
    public void cannot_assign_the_room_before_checkin_and_checkout_times_are_known() {
        given(new SearchedForAccommodation(id, arrival, departure));

        var e = assertThrows(IllegalStateException.class, () -> {
            when(new AssignRoom(id, roomId2));
        });
        assertThat(e.getMessage(), is("unexpected state: PROSPECTIVE"));
    }

    @Test
    public void validates_the_roomId() {
        var roomId3 = UUID.randomUUID();

        var e = assertThrows(IllegalArgumentException.class, () -> {
            when(new AssignRoom(id, roomId3));
        });
        assertThat(e.getMessage(), is("room not found: " + roomId3));
    }
}
