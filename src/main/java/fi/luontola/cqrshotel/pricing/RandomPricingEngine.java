// Copyright © 2016 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.pricing;

import org.javamoney.moneta.Money;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public class RandomPricingEngine implements PricingEngine {

    public static final int MAX_DAYS_IN_FUTURE = 365;

    private final Clock clock;

    public RandomPricingEngine(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Optional<Money> getAccommodationPrice(LocalDate date) {
        LocalDate limit = LocalDate.now(clock).plusDays(MAX_DAYS_IN_FUTURE);
        if (date.isBefore(limit)) {
            return Optional.of(Money.of(ThreadLocalRandom.current().nextInt(50, 150), "EUR"));
        } else {
            return Optional.empty();
        }
    }
}
