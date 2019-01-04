// Copyright © 2016-2019 Esko Luontola
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
        items.add(new WeightedItem<>(weight, sumOfWeights() + weight, item));
    }

    public T next() {
        return next(ThreadLocalRandom.current().nextDouble());
    }

    public T next(double random) {
        if (random < 0 || random > 1) {
            throw new IllegalArgumentException("expected random number between 0 and 1, but was " + random);
        }
        var cursor = random * sumOfWeights();
        for (var item : items) { // we assume a low number of items, so use linear instead of binary search
            if (cursor <= item.cumulativeWeight) {
                return item.value;
            }
        }
        throw new IllegalStateException("no items");
    }

    private double sumOfWeights() {
        return lastItem().cumulativeWeight;
    }

    private WeightedItem<T> lastItem() {
        var size = items.size();
        if (size == 0) {
            return new WeightedItem<>(0, 0, null);
        } else {
            return items.get(size - 1);
        }
    }

    @Override
    public String toString() {
        var sj = new StringJoiner(", ", "{", "}");
        for (var item : items) {
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
