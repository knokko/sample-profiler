package com.github.knokko.profiler.storage;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestTimelineThreadStorage {

    @Test
    public void testSampleAndGetBetween() {
        TimelineThreadStorage storage = new TimelineThreadStorage();
        assertEquals(0, storage.getBetween(123, 1234).size());
        assertEquals(0, storage.sample(123, 1234).length);

        StackTraceElement add = new StackTraceElement("Adder", "add", "Adder", 1);
        StackTraceElement back = new StackTraceElement("Backing", "goBack", "Backing", 2);
        StackTraceElement cast = new StackTraceElement("Caster", "castSpell", "Caster", 3);
        StackTraceElement dog = new StackTraceElement("Animal", "dog", "Animal", 4);

        StackTraceElement[] trace1 = { dog, add, back };
        storage.insert(trace1, 100);

        assertEquals(0, storage.getBetween(0, 99).size());
        assertEquals(1, storage.getBetween(50, 100).size());
        assertArrayEquals(trace1, storage.getBetween(100, 110).get(0).stackTrace);
        assertEquals(100, storage.getBetween(95, 120).get(0).timestamp);
        assertEquals(0, storage.getBetween(101, 200).size());

        assertEquals(0, storage.sample(0, 99).length);
        assertArrayEquals(trace1, storage.sample(0, 100));
        assertArrayEquals(trace1, storage.sample(100, 300));
        assertEquals(0, storage.sample(101, 300).length);

        StackTraceElement[] trace2 = { cast, add, back };

        storage.insert(trace2, 90);
        assertEquals(0, storage.getBetween(0, 89).size());
        assertEquals(1, storage.getBetween(50, 90).size());
        assertEquals(1, storage.getBetween(50, 99).size());
        assertEquals(2, storage.getBetween(50, 100).size());

        List<TimelineThreadStorage.Sample> samples = storage.getBetween(90, 100);
        assertEquals(2, samples.size());
        assertEquals(90, samples.get(0).timestamp);
        assertArrayEquals(trace2, samples.get(0).stackTrace);
        assertEquals(100, samples.get(1).timestamp);
        assertArrayEquals(trace1, samples.get(1).stackTrace);

        assertEquals(1, storage.getBetween(91, 100).size());
        assertEquals(1, storage.getBetween(100, 101).size());
        assertEquals(0, storage.getBetween(101, 101).size());

        StackTraceElement[] combinedTrace = { add, back };

        assertEquals(0, storage.sample(50, 89).length);
        assertArrayEquals(trace2, storage.sample(80, 90));
        assertArrayEquals(trace2, storage.sample(90, 90));
        assertArrayEquals(combinedTrace, storage.sample(90, 91));
        assertArrayEquals(combinedTrace, storage.sample(90, 99));
        assertArrayEquals(combinedTrace, storage.sample(90, 100));
        assertArrayEquals(combinedTrace, storage.sample(91, 100));
        assertArrayEquals(trace1, storage.sample(100, 100));
        assertEquals(0, storage.sample(101, 200).length);
    }

    @Test
    public void testGetIntervalsBetween() {
        StackTraceElement main1 = new StackTraceElement(
                "SampleProfiler", "main", "SampleProfiler", 1
        );
        StackTraceElement main2 = new StackTraceElement(
                "SampleProfiler", "main", "SampleProfiler", 2
        );

        StackTraceElement run = new StackTraceElement(
                "Application", "run", "Application", 100
        );

        StackTraceElement[] runMain1 = { run, main1 };
        StackTraceElement[] runMain2 = { run, main2 };
        StackTraceElement[] other = { new StackTraceElement(
                "SomethingElse", "other", "SomethingElse", 100
        ) };

        TimelineThreadStorage storage = new TimelineThreadStorage();
        storage.insert(runMain1, 10);
        storage.insert(runMain1, 20);
        storage.insert(runMain2, 30);
        storage.insert(other, 40);
        storage.insert(runMain1, 50);

        assertEquals(0, storage.getIntervalsBetween(0, 19).size());

        List<TimelineThreadStorage.Interval> intervals1 = storage.getIntervalsBetween(10, 20);
        assertEquals(1, intervals1.size());
        assertEquals(new TimelineThreadStorage.Interval(10, 20, runMain1), intervals1.get(0));
        assertEquals(intervals1, storage.getIntervalsBetween(0, 20));
        assertEquals(intervals1, storage.getIntervalsBetween(10, 29));

        List<TimelineThreadStorage.Interval> intervals2 = storage.getIntervalsBetween(10, 30);
        assertEquals(1, intervals2.size());
        assertEquals(new TimelineThreadStorage.Interval(10, 30, runMain1), intervals2.get(0));
        assertEquals(intervals2, storage.getIntervalsBetween(5, 39));

        List<TimelineThreadStorage.Interval> intervals3 = storage.getIntervalsBetween(10, 40);
        assertEquals(2, intervals3.size());
        assertEquals(intervals2.get(0), intervals3.get(0));
        assertEquals(new TimelineThreadStorage.Interval(30, 40, runMain2), intervals3.get(1));
        assertEquals(intervals3, storage.getIntervalsBetween(0, 49));

        List<TimelineThreadStorage.Interval> intervals4 = storage.getIntervalsBetween(10, 50);
        assertEquals(3, intervals4.size());
        assertEquals(intervals3.get(0), intervals4.get(0));
        assertEquals(intervals3.get(1), intervals4.get(1));
        assertEquals(new TimelineThreadStorage.Interval(40, 50, other), intervals4.get(2));
        assertEquals(intervals4, storage.getIntervalsBetween(-100, 100));

        List<TimelineThreadStorage.Interval> intervals5 = storage.getIntervalsBetween(25, 75);
        assertEquals(2, intervals5.size());
        assertEquals(intervals4.get(1), intervals5.get(0));
        assertEquals(intervals4.get(2), intervals5.get(1));
    }
}
