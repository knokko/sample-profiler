package com.github.knokko.profiler.storage;

public interface ThreadStorage {

    void insert(StackTraceElement[] stackTrace, long timestamp);
}
