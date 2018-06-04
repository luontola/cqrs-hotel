// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.luontola.cqrshotel.framework.Message;
import org.apache.commons.lang3.RandomStringUtils;
import org.javamoney.moneta.Money;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.reflections.Reflections;

import javax.money.Monetary;
import java.lang.reflect.Constructor;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@Category(SlowTests.class)
public class JsonSerializationTest {

    private final ObjectMapper objectMapper = new Application().jacksonObjectMapper();

    @Test
    public void LocalDate_format() throws JsonProcessingException {
        assertThat(objectMapper.writeValueAsString(LocalDate.of(2000, 1, 2)), is("\"2000-01-02\""));
    }

    @Test
    public void Instant_format() throws JsonProcessingException {
        assertThat(objectMapper.writeValueAsString(Instant.ofEpochSecond(0)), is("\"1970-01-01T00:00:00Z\""));
    }

    @Test
    public void Money_format() throws JsonProcessingException {
        assertThat(objectMapper.writeValueAsString(Money.of(12.34, "EUR")), is("\"EUR 12.34\""));
    }

    @Test
    public void all_messages_are_serializable() {
        new Reflections("fi.luontola.cqrshotel")
                .getSubTypesOf(Message.class).stream()
                .filter(type -> !type.isInterface())
                .filter(type -> !isTestDouble(type))
                .forEach(type -> assertSerializable(type, objectMapper));
    }

    private static boolean isTestDouble(Class<? extends Message> type) {
        return type.isMemberClass() &&
                Stream.of(type.getEnclosingClass().getMethods())
                        .anyMatch(method -> method.isAnnotationPresent(Test.class));
    }

    private static void assertSerializable(Class<?> type, ObjectMapper objectMapper) {
        try {
            Object original = newDummy(type);
            String json = objectMapper.writeValueAsString(original);
            Object deserialized = objectMapper.readValue(json, type);
            assertThat(deserialized, is(original));
        } catch (Exception e) {
            e.printStackTrace();
            throw new AssertionError("Not serializable: " + type, e);
        }
    }

    private static Object newDummy(Class<?> type) throws Exception {
        Constructor<?> ctor = type.getConstructors()[0];
        Class<?>[] paramTypes = ctor.getParameterTypes();
        Object[] params = new Object[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            params[i] = randomValue(paramTypes[i]);
        }
        return ctor.newInstance(params);
    }

    private static Object randomValue(Class<?> type) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (type == UUID.class) {
            return UUID.randomUUID();
        }
        if (type == LocalDate.class) {
            return LocalDate.of(
                    random.nextInt(2000, 2100),
                    random.nextInt(Month.JANUARY.getValue(), Month.DECEMBER.getValue() + 1),
                    random.nextInt(1, Month.FEBRUARY.minLength() + 1));
        }
        if (type == Money.class) {
            return Money.of(
                    random.nextDouble(0, 1000),
                    pickRandom(Monetary.getCurrencies()));
        }
        if (type == Instant.class) {
            return Instant.ofEpochMilli(random.nextLong());
        }
        if (type == ZonedDateTime.class) {
            ZoneId zoneId = ZoneId.of(pickRandom(ZoneId.getAvailableZoneIds()));
            return Instant.ofEpochMilli(random.nextLong()).atZone(zoneId);
        }
        if (type == String.class) {
            return RandomStringUtils.randomAlphanumeric(random.nextInt(10));
        }
        if (type == int.class) {
            return random.nextInt();
        }
        if (type == Class.class) {
            return pickRandom(Arrays.asList(Integer.class, Long.class, Float.class, Double.class));
        }
        throw new IllegalArgumentException("Unsupported type: " + type);
    }

    private static <T> T pickRandom(Collection<T> values) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return values.stream()
                .skip(random.nextInt(values.size()))
                .findFirst()
                .get();
    }
}
