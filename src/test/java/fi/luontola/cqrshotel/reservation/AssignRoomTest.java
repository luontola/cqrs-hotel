// Copyright © 2016-2019 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.reservation;

import fi.luontola.cqrshotel.FastTests;
import fi.luontola.cqrshotel.framework.AggregateRootTester;
import fi.luontola.cqrshotel.hotel.Hotel;
import fi.luontola.cqrshotel.reservation.commands.AssignRoom;
import fi.luontola.cqrshotel.reservation.commands.AssignRoomHandler;
import fi.luontola.cqrshotel.reservation.events.ReservationInitiated;
import fi.luontola.cqrshotel.reservation.events.RoomAssigned;
import fi.luontola.cqrshotel.reservation.events.SearchedForAccommodation;
import fi.luontola.cqrshotel.room.events.RoomCreated;
import fi.luontola.cqrshotel.room.queries.GetRoomByIdHandler;
import fi.luontola.cqrshotel.room.queries.RoomsView;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;

import java.time.LocalDate;
import java.util.UUID;

@Category(FastTests.class)
public class AssignRoomTest extends AggregateRootTester {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    private static final LocalDate arrival = LocalDate.of(2000, 1, 1);
    private static final LocalDate departure = LocalDate.of(2000, 1, 5);
    private static final UUID roomId = UUID.randomUUID();
    private static final UUID roomId2 = UUID.randomUUID();
    private static final String roomNumber = "101";
    private static final String roomNumber2 = "102";

    {
        RoomsView roomsView = new RoomsView();
        roomsView.apply(new RoomCreated(roomId, roomNumber));
        roomsView.apply(new RoomCreated(roomId2, roomNumber2));
        GetRoomByIdHandler getRoomById = new GetRoomByIdHandler(roomsView);
        commandHandler = new AssignRoomHandler(new ReservationRepo(eventStore), getRoomById);
    }

    @Test
    public void assigns_the_room_to_the_reservation() {
        given(new ReservationInitiated(id, arrival, departure, Hotel.checkInTime(arrival), Hotel.checkOutTime(departure)));

        when(new AssignRoom(id, roomId));

        then(new RoomAssigned(id, roomId, roomNumber));
    }

    @Test
    public void the_assigned_room_can_be_changed() {
        given(new ReservationInitiated(id, arrival, departure, Hotel.checkInTime(arrival), Hotel.checkOutTime(departure)),
                new RoomAssigned(id, roomId, roomNumber));

        when(new AssignRoom(id, roomId2));

        then(new RoomAssigned(id, roomId2, roomNumber2));
    }

    @Test
    public void cannot_assign_the_room_before_checkin_and_checkout_times_are_known() {
        given(new SearchedForAccommodation(id, arrival, departure));

        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("unexpected state: PROSPECTIVE");
        when(new AssignRoom(id, roomId2));
    }

    @Test
    public void validates_the_roomId() {
        UUID roomId3 = UUID.randomUUID();

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("room not found: " + roomId3);
        when(new AssignRoom(id, roomId3));
    }
}
