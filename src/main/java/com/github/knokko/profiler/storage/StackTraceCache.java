package com.github.knokko.profiler.storage;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

class StackTraceCache {

    private final ConcurrentMap<StackTrace, StackTraceElement[]> map = new ConcurrentHashMap<>();

    // TODO Test this
    public StackTraceElement[] getCached(StackTraceElement[] stackTrace) {
        return map.computeIfAbsent(new StackTrace(stackTrace), key -> stackTrace);
    }

    private static class StackTrace {

        final StackTraceElement[] elements;

        StackTrace(StackTraceElement[] elements) {
            this.elements = elements;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof StackTrace && Arrays.equals(this.elements, ((StackTrace) other).elements);
        }

        @Override
        public int hashCode() {
            int hashCode = 0;
            for (StackTraceElement element : elements) hashCode += element.hashCode();
            return hashCode;
        }
    }
}
