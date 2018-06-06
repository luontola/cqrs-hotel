// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework.eventstore;

import fi.luontola.cqrshotel.framework.Envelope;
import fi.luontola.cqrshotel.framework.Event;
import fi.luontola.cqrshotel.framework.eventstore.EventStoreContract.DummyEvent;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@State(Scope.Benchmark)
public class InMemoryEventStore_ReadRecentEventsBenchmark {

    @Param({
            "1000",
            "10000",
            "100000",
            "1000000",
    })
    public int eventCount;

    private final InMemoryEventStore eventStore = new InMemoryEventStore();
    private final UUID streamId = UUID.randomUUID();
    private int readPosition;

    @Setup
    public void prepare() {
        Envelope<Event> event = Envelope.newMessage(new DummyEvent(""));
        List<Envelope<Event>> events = Stream.generate(() -> event)
                .limit(eventCount)
                .collect(Collectors.toList());
        long endPosition = eventStore.saveEvents(streamId, events, EventStore.BEGINNING);
        readPosition = (int) endPosition - 10;
    }

    @Benchmark
    public List<PersistedEvent> getEventsForStream() {
        return eventStore.getEventsForStream(streamId, readPosition);
    }

    @Benchmark
    public List<PersistedEvent> getAllEvents() {
        return eventStore.getAllEvents(readPosition);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(InMemoryEventStore_ReadRecentEventsBenchmark.class.getSimpleName())
                .warmupIterations(5)
                .measurementIterations(5)
                .forks(1)
                .build();
        new Runner(opt).run();
    }
}
