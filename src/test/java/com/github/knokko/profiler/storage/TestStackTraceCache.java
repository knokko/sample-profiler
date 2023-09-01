package com.github.knokko.profiler.storage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

public class TestStackTraceCache {

    @Test
    public void testGetCached() {
        StackTraceElement attack = new StackTraceElement("Attacker", "attack", "Attacker", 1);
        StackTraceElement base = new StackTraceElement("Base", "base", "Base", 2);

        StackTraceElement[] trace1a = { attack, attack, base };
        StackTraceElement[] trace1b = { attack, attack, base };
        StackTraceElement[] trace2a = { attack, base };
        StackTraceElement[] trace2b = { attack, base };

        StackTraceCache cache = new StackTraceCache();
        assertSame(trace1a, cache.getCached(trace1a));
        assertSame(trace1a, cache.getCached(trace1b));
        assertSame(trace1a, cache.getCached(trace1a));

        assertSame(trace2a, cache.getCached(trace2a));
        assertSame(trace2a, cache.getCached(trace2b));
        assertSame(trace2a, cache.getCached(trace2a));
    }
}
