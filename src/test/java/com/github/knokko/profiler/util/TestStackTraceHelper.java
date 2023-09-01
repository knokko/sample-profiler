package com.github.knokko.profiler.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.github.knokko.profiler.util.StackTraceHelper.longestCommonStackTrace;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestStackTraceHelper {

    @Test
    public void testLongestCommonStackTraceEmptyCollection() {
        assertEquals(0, longestCommonStackTrace(Collections.emptyIterator()).length);
    }

    @Test
    public void testLongestCommonStackTraceEmptyTraces() {
        assertEquals(0, longestCommonStackTrace(Collections.singletonList(new StackTraceElement[0]).iterator()).length);

        List<StackTraceElement[]> list = new ArrayList<>();
        list.add(new StackTraceElement[]{ new StackTraceElement("test", "test", "test", 1) });
        list.add(new StackTraceElement[0]);

        assertEquals(0, longestCommonStackTrace(list.iterator()).length);
        Collections.reverse(list);
        assertEquals(0, longestCommonStackTrace(list.iterator()).length);
    }

    @Test
    public void testLongestCommonStackTraceActualTraces() {
        StackTraceElement[] baseTrace = take(1);

        List<StackTraceElement[]> stackTraces = new ArrayList<>();

        a(stackTraces);
        assertArrayEquals(stackTraces.get(0), longestCommonStackTrace(stackTraces.iterator()));

        b(stackTraces);
        assertArrayEquals(baseTrace, longestCommonStackTrace(stackTraces.iterator()));

        stackTraces.add(baseTrace);
        assertArrayEquals(baseTrace, longestCommonStackTrace(stackTraces.iterator()));
    }

    private StackTraceElement[] take(int extraDiscard) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        return Arrays.copyOfRange(stackTrace, 2 + extraDiscard, stackTrace.length);
    }

    private void a(List<StackTraceElement[]> stackTraces) {
        stackTraces.add(take(1));
        aa(stackTraces);
    }

    private void b(List<StackTraceElement[]> stackTraces) {
        stackTraces.add(take(0));
    }

    private void aa(List<StackTraceElement[]> stackTraces) {
        stackTraces.add(take(0));
    }
}
