// Copyright © 2016 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel;

import fi.luontola.cqrshotel.commands.MakeReservation;
import fi.luontola.cqrshotel.events.ContactInformationUpdated;
import fi.luontola.cqrshotel.events.ReservationMade;
import fi.luontola.cqrshotel.framework.Event;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
public class ApiController {

    public static final ZoneId TIMEZONE = ZoneId.systemDefault();
    public static final LocalTime CHECK_IN_TIME = LocalTime.of(14, 0);
    public static final LocalTime CHECK_OUT_TIME = LocalTime.of(12, 0);

    @RequestMapping(path = "/api", method = GET)
    public String home() {
        return "CQRS Hotel API";
    }

    @RequestMapping(path = "/api/dummy", method = GET)
    public List<String> dummy() {
        return Arrays.asList("foo", "bar", "gazonk");
    }

    @RequestMapping(path = "/api/make-reservation", method = POST,
            produces = MimeTypeUtils.APPLICATION_JSON_VALUE,
            consumes = MimeTypeUtils.APPLICATION_JSON_VALUE)
    public List<Event> makeReservation(@RequestBody MakeReservation command) {
        System.out.println("ApiController.makeReservation");
        System.out.println("command = " + command);
        UUID reservationId = UUID.randomUUID();
        Instant checkInTime = command.startDate
                .atTime(CHECK_IN_TIME)
                .atZone(TIMEZONE)
                .toInstant();
        Instant checkOutTime = command.endDate
                .atTime(CHECK_OUT_TIME)
                .atZone(TIMEZONE)
                .toInstant();
        return Arrays.asList(
                new ContactInformationUpdated(reservationId, command.name, command.email),
                new ReservationMade(reservationId, checkInTime, checkOutTime)
        );
    }
}
