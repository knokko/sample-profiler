package com.github.knokko.profiler.storage;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestSampleStorage {

    @Test
    public void testSampleStorage() {
        DummyStorage storage1 = new DummyStorage();
        DummyStorage storage2 = new DummyStorage();

        List<DummyStorage> list = new ArrayList<>();
        list.add(storage1);
        list.add(storage2);

        SampleStorage<DummyStorage> storage = new SampleStorage<>(() -> list.remove(0));
        StackTraceElement[] dummyStackTrace = {};

        assertNull(storage.getThreadStorage(12));
        storage.insert(12, dummyStackTrace, 1234);
        assertEquals(1, storage1.counter);
        assertSame(storage1, storage.getThreadStorage(12));

        storage.insert(12, dummyStackTrace, 12);
        assertEquals(2, storage1.counter);
        assertEquals(0, storage2.counter);
        assertNull(storage.getThreadStorage(5));

        storage.insert(5, dummyStackTrace, 80);
        assertSame(storage2, storage.getThreadStorage(5));
        assertEquals(1, storage2.counter);

        storage.insert(12, dummyStackTrace, 33);
        storage.insert(5, dummyStackTrace, 30);
        assertEquals(3, storage1.counter);
        assertEquals(2, storage2.counter);
    }

    private static class DummyStorage implements ThreadStorage {

        int counter = 0;

        @Override
        public void insert(StackTraceElement[] stackTrace, long timestamp) {
            counter += 1;
        }
    }
}
