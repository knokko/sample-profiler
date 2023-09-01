package com.github.knokko.profiler.storage;

import com.github.knokko.profiler.util.StackTraceHelper;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

public class TimelineThreadStorage implements ThreadStorage {

    private static final StackTraceElement[] DUMMY_TRACE = {};

    private final NavigableSet<Sample> samples = new ConcurrentSkipListSet<>();
    private final StackTraceCache cache = new StackTraceCache();

    @Override
    public void insert(StackTraceElement[] stackTrace, long timestamp) {
        samples.add(new Sample(cache.getCached(stackTrace), timestamp));
    }

    public List<Sample> getBetween(long startTime, long endTime) {
        return new ArrayList<>(samples.subSet(
                new Sample(DUMMY_TRACE, startTime), true, new Sample(DUMMY_TRACE, endTime), true
        ));
    }

    public List<Interval> getIntervalsBetween(long startTime, long endTime) {
        List<Sample> samples = getBetween(startTime, endTime);
        if (samples.isEmpty()) return new ArrayList<>();

        List<Interval> intervals = new ArrayList<>();

        Sample previousSample = samples.get(0);
        for (Sample sample : samples) {
            if (Arrays.equals(sample.stackTrace, previousSample.stackTrace)) continue;
            intervals.add(new Interval(previousSample.timestamp, sample.timestamp, previousSample.stackTrace));
            previousSample = sample;
        }

        Sample lastSample = samples.get(samples.size() - 1);
        if (lastSample != previousSample) intervals.add(new Interval(previousSample.timestamp, lastSample.timestamp, lastSample.stackTrace));

        return intervals;
    }

    public StackTraceElement[] sample(long startTime, long endTime) {
        Sample startSample = samples.floor(new Sample(DUMMY_TRACE, startTime));
        if (startSample == null) {
            try {
                Sample first = samples.first();
                if (first.timestamp <= endTime) startSample = first;
            } catch (NoSuchElementException ignored) {}
        }
        if (startSample == null) return new StackTraceElement[0];

        Sample endSample = samples.ceiling(new Sample(DUMMY_TRACE, endTime));
        if (endSample == null) {
            try {
                Sample last = samples.last();
                if (last.timestamp >= startTime) endSample = last;
            } catch (NoSuchElementException ignored) {}
        }
        if (endSample == null) return new StackTraceElement[0];

        Collection<Sample> relevantSamples = samples.subSet(startSample, true, endSample, true);
        return StackTraceHelper.longestCommonStackTrace(relevantSamples.stream().map(sample -> sample.stackTrace).iterator());
    }

    public static class Sample implements Comparable<Sample> {

        public final StackTraceElement[] stackTrace;
        public final long timestamp;

        public Sample(StackTraceElement[] stackTrace, long timestamp) {
            this.stackTrace = Objects.requireNonNull(stackTrace);
            this.timestamp = timestamp;
        }

        @Override
        public int compareTo(Sample other) {
            return Long.compare(this.timestamp, other.timestamp);
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof Sample) {
                Sample otherSample = (Sample) other;
                return this.timestamp == otherSample.timestamp && Arrays.equals(this.stackTrace, otherSample.stackTrace);
            } else return false;
        }

        @Override
        public String toString() {
            return "Sample(" + timestamp + ": " + Arrays.toString(stackTrace) + ")";
        }
    }

    public static class Interval {

        public final long startTime, endTime;
        public final StackTraceElement[] stackTrace;

        public Interval(long startTime, long endTime, StackTraceElement[] stackTrace) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.stackTrace = stackTrace;
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof Interval) {
                Interval otherInterval = (Interval) other;
                return startTime == otherInterval.startTime && endTime == otherInterval.endTime
                        && Arrays.equals(stackTrace, otherInterval.stackTrace);
            } else return false;
        }

        @Override
        public String toString() {
            StackTraceElement[] reversedStackTrace = new StackTraceElement[stackTrace.length];
            for (int index = 0; index < stackTrace.length; index++) {
                reversedStackTrace[index] = stackTrace[stackTrace.length - 1 - index];
            }
            return String.format(
                    "%d - %d (%.3f ms): %s", startTime, endTime, (endTime - startTime) / 1_000_000.0,
                    Arrays.toString(reversedStackTrace)
            );
        }
    }
}
