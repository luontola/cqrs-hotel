// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@State(Scope.Benchmark)
public class ExceptionBenchmark {

    public String data;
    public RuntimeException reused;

    @Setup
    public void prepare() {
        data = "foo";
        reused = new RuntimeException(data);
    }

    @Benchmark
    public String tryCatchNew() {
        try {
            throw new RuntimeException(data);
        } catch (RuntimeException e) {
            return e.getMessage();
        }
    }

    @Benchmark
    public String tryCatchReused() {
        try {
            throw reused;
        } catch (RuntimeException e) {
            return e.getMessage();
        }
    }

    @Benchmark
    public String tryCatchNoStackTrace() {
        try {
            throw new NoStackTraceException(data);
        } catch (RuntimeException e) {
            return e.getMessage();
        }
    }

    @Benchmark
    public String noException() {
        if (data.length() > 1) {
            return data;
        } else {
            return null;
        }
    }

    public static class NoStackTraceException extends RuntimeException {
        public NoStackTraceException(String s) {
            super(s);
        }

        @Override
        public Throwable fillInStackTrace() {
            return this;
        }
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(ExceptionBenchmark.class.getSimpleName())
                .warmupIterations(5)
                .measurementIterations(5)
                .forks(1)
                .build();
        new Runner(opt).run();
    }
}
