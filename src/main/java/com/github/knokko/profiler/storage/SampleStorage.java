package com.github.knokko.profiler.storage;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

public class SampleStorage<T extends ThreadStorage> {

    public static SampleStorage<FrequencyThreadStorage> frequency() {
        return new SampleStorage<>(FrequencyThreadStorage::new);
    }

    public static SampleStorage<TimelineThreadStorage> timeline() {
        return new SampleStorage<>(TimelineThreadStorage::new);
    }

    private final ConcurrentMap<Long, T> threads = new ConcurrentHashMap<>();
    private final Supplier<T> createThreadStorage;

    public SampleStorage(Supplier<T> createThreadStorage) {
        this.createThreadStorage = createThreadStorage;
    }

    public void insert(long threadID, StackTraceElement[] stackTrace, long timestamp) {
        T threadStorage = threads.computeIfAbsent(threadID, key -> createThreadStorage.get());
        if (threadStorage == null) threadStorage = threads.get(threadID);
        threadStorage.insert(stackTrace, timestamp);
    }

    public T getThreadStorage(long threadID) {
        return threads.get(threadID);
    }
}
