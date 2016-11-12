// Copyright © 2016 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel;

import fi.luontola.cqrshotel.framework.EventStore;
import fi.luontola.cqrshotel.framework.InMemoryEventStore;
import fi.luontola.cqrshotel.pricing.PricingEngine;
import fi.luontola.cqrshotel.pricing.RandomPricingEngine;
import fi.luontola.cqrshotel.reservation.ReservationRepo;
import fi.luontola.cqrshotel.reservation.commands.MakeReservation;
import fi.luontola.cqrshotel.reservation.commands.MakeReservationHandler;
import fi.luontola.cqrshotel.reservation.commands.SearchForAccommodation;
import fi.luontola.cqrshotel.reservation.commands.SearchForAccommodationHandler;
import fi.luontola.cqrshotel.reservation.queries.ReservationOffer;
import org.javamoney.moneta.Money;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
public class ApiController {

    private final EventStore eventStore = new InMemoryEventStore();
    private final ReservationRepo reservationRepo = new ReservationRepo(eventStore);
    private final Clock clock = Clock.systemDefaultZone();
    private final PricingEngine pricing = new RandomPricingEngine(clock);

    @RequestMapping(path = "/api", method = GET)
    public String home() {
        return "CQRS Hotel API";
    }

    @RequestMapping(path = "/api/dummy", method = GET)
    public List<String> dummy() {
        return Arrays.asList("foo", "bar", "gazonk");
    }

    @RequestMapping(path = "/api/search-for-accommodation", method = POST)
    public ReservationOffer findAvailableRoom(@RequestBody SearchForAccommodation command) {

        // TODO: composite handler
        new SearchForAccommodationHandler(reservationRepo, pricing, clock).handle(command);

        // TODO: read model
        ReservationOffer result = new ReservationOffer();
        result.reservationId = command.reservationId;
        result.startDate = command.startDate;
        result.endDate = command.endDate;
        result.totalPrice = Money.of(ThreadLocalRandom.current().nextInt(250, 500), "EUR");
        return result;
    }

    @RequestMapping(path = "/api/make-reservation", method = POST)
    public void makeReservation(@RequestBody MakeReservation command) {
        // TODO: composite handler
        new MakeReservationHandler(reservationRepo).handle(command);
    }
}
