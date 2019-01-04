// Copyright © 2016-2019 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fi.luontola.cqrshotel.framework.consistency.ObservedPosition;
import fi.luontola.cqrshotel.framework.eventstore.EventStore;
import fi.luontola.cqrshotel.framework.eventstore.PsqlEventStore;
import fi.luontola.cqrshotel.framework.util.UUIDs;
import fi.luontola.cqrshotel.pricing.PricingEngine;
import fi.luontola.cqrshotel.pricing.RandomPricingEngine;
import fi.luontola.cqrshotel.room.commands.CreateRoom;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;

@SpringBootApplication
public class Application {

    private static final Duration QUERY_TIMEOUT = Duration.ofSeconds(15);

    public static void main(String[] args) throws Exception {
        var app = SpringApplication.run(Application.class, args);
        var eventStore = app.getBean(EventStore.class);
        if (eventStore.getCurrentPosition() == 0) {
            var api = app.getBean(ApiController.class);
            initializeTestData(api);
        }
    }

    private static void initializeTestData(ApiController api) {
        for (var roomNumber : Arrays.asList("101", "102", "103", "104", "105")) {
            api.createRoom(new CreateRoom(UUIDs.newUUID(), roomNumber));
        }
    }

    @Bean
    public Core core(EventStore eventStore, PricingEngine pricing, Clock clock, ObservedPosition observedPosition) {
        return new Core(eventStore, pricing, clock, observedPosition);
    }

    @Bean
    public EventStore eventStore(DataSource dataSource, ObjectMapper objectMapper) {
        return new PsqlEventStore(dataSource, objectMapper);
    }

    @Bean
    public PricingEngine pricingEngine(Clock clock) {
        return new RandomPricingEngine(clock);
    }

    @Bean
    public ObservedPosition observedPosition() {
        return new ObservedPosition(QUERY_TIMEOUT);
    }

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public ObjectMapper jacksonObjectMapper() {
        var om = new ObjectMapper();
        om.registerModules(new JavaTimeModule(), new MoneyModule());
        om.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        om.configure(SerializationFeature.INDENT_OUTPUT, true);
        om.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        om.configure(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS, true);
        // preserve time zone information
        om.configure(SerializationFeature.WRITE_DATES_WITH_ZONE_ID, true);
        om.configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false);
        return om;
    }
}
