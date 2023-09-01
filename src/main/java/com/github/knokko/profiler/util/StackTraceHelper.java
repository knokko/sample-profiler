package com.github.knokko.profiler.util;

import java.util.Arrays;
import java.util.Iterator;

public class StackTraceHelper {

    public static StackTraceElement[] longestCommonStackTrace(Iterator<StackTraceElement[]> stackTraces) {
        if (!stackTraces.hasNext()) return new StackTraceElement[0];

        StackTraceElement[] firstStackTrace = stackTraces.next();
        if (firstStackTrace.length == 0) return new StackTraceElement[0];
        int startIndex = 0;

        while (stackTraces.hasNext()) {
            StackTraceElement[] otherStackTrace = stackTraces.next();
            int otherIndex = startIndex + otherStackTrace.length - firstStackTrace.length;

            while (otherIndex < 0 || !firstStackTrace[startIndex].equals(otherStackTrace[otherIndex])) {
                otherIndex += 1;
                startIndex += 1;
                if (startIndex >= firstStackTrace.length) return new StackTraceElement[0];
            }
        }

        return Arrays.copyOfRange(firstStackTrace, startIndex, firstStackTrace.length);
    }
}
