// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.testing;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.ThreadLocalRandom;

public class WeightedRandom<T> {

    private final List<WeightedItem<T>> items = new ArrayList<>();

    public void add(double weight, T item) {
        items.add(new WeightedItem<>(weight, getSumOfWeights() + weight, item));
    }

    public T next() {
        return next(ThreadLocalRandom.current().nextDouble());
    }

    public T next(double random) {
        if (random < 0 || random > 1) {
            throw new IllegalArgumentException("expected random number between 0 and 1, but was " + random);
        }
        double cursor = random * getSumOfWeights();
        for (WeightedItem<T> item : items) {
            if (cursor <= item.cumulativeWeight) {
                return item.value;
            }
        }
        throw new IllegalStateException("no items");
    }

    private double getSumOfWeights() {
        int lastIndex = items.size() - 1;
        if (lastIndex < 0) {
            return 0;
        } else {
            return items.get(lastIndex).cumulativeWeight;
        }
    }

    @Override
    public String toString() {
        StringJoiner sj = new StringJoiner(", ", "{", "}");
        for (WeightedItem<T> item : items) {
            sj.add(item.weight + "=" + item.value);
        }
        return sj.toString();
    }

    private static class WeightedItem<T> {
        final double weight;
        final double cumulativeWeight;
        final T value;

        public WeightedItem(double weight, double cumulativeWeight, T value) {
            this.weight = weight;
            this.cumulativeWeight = cumulativeWeight;
            this.value = value;
        }
    }
}
