// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework.consistency;

import fi.luontola.cqrshotel.FastTests;
import fi.luontola.cqrshotel.framework.InMemoryEventStore;
import fi.luontola.cqrshotel.framework.InMemoryProjectionUpdater;
import fi.luontola.cqrshotel.room.queries.RoomsView;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

@Category(FastTests.class)
public class ObservedPositionTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void starts_with_zero() {
        ObservedPosition observedPosition = new ObservedPosition(Duration.ZERO);

        assertThat(observedPosition.get(), is(0L));
    }

    @Test
    public void increases_when_observing_higher_values() {
        ObservedPosition observedPosition = new ObservedPosition(Duration.ZERO);
        observedPosition.observe(1L);
        assertThat(observedPosition.get(), is(1L));

        observedPosition.observe(2L);
        assertThat(observedPosition.get(), is(2L));
    }

    @Test
    public void stays_same_when_observing_lower_values() {
        ObservedPosition observedPosition = new ObservedPosition(Duration.ZERO);
        observedPosition.observe(2L);
        assertThat(observedPosition.get(), is(2L));

        observedPosition.observe(1L);
        assertThat(observedPosition.get(), is(2L));
    }

    @Test
    public void reset_brings_it_to_zero() {
        ObservedPosition observedPosition = new ObservedPosition(Duration.ZERO);
        observedPosition.observe(10L);
        assertThat(observedPosition.get(), is(10L));

        observedPosition.reset();
        assertThat(observedPosition.get(), is(0L));
    }

    @Test
    public void value_is_thread_local() throws InterruptedException {
        ObservedPosition observedPosition = new ObservedPosition(Duration.ZERO);
        CyclicBarrier barrier = new CyclicBarrier(3);
        Long[] results = new Long[2];

        new Thread(() -> {
            observedPosition.observe(10L);
            sync(barrier);
            results[0] = observedPosition.get();
            sync(barrier);
        }).start();
        new Thread(() -> {
            observedPosition.observe(20L);
            sync(barrier);
            results[1] = observedPosition.get();
            sync(barrier);
        }).start();
        sync(barrier); // both have observed a (different) value
        sync(barrier); // both have read the current value

        assertThat(Arrays.asList(results), is(Arrays.asList(10L, 20L)));
    }

    private static void sync(CyclicBarrier barrier) {
        try {
            barrier.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (BrokenBarrierException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void waiting_returns_silently_if_projection_is_up_to_date() {
        ObservedPosition observedPosition = new ObservedPosition(Duration.ZERO);
        InMemoryProjectionUpdater projection = new InMemoryProjectionUpdater(new RoomsView(), new InMemoryEventStore());
        assertThat(observedPosition.get(), is(projection.getPosition()));

        observedPosition.waitForProjectionToUpdate(projection);
    }

    @Test
    public void waiting_throws_exception_if_projection_update_times_out() {
        ObservedPosition observedPosition = new ObservedPosition(Duration.ZERO);
        InMemoryProjectionUpdater projection = new InMemoryProjectionUpdater(new RoomsView(), new InMemoryEventStore());
        observedPosition.observe(1);
        assertThat(observedPosition.get(), is(greaterThan(projection.getPosition())));

        thrown.expect(ReadModelNotUpToDateException.class);
        observedPosition.waitForProjectionToUpdate(projection);
    }
}