package com.github.knokko.profiler;

import com.github.knokko.profiler.storage.FrequencyThreadStorage;
import com.github.knokko.profiler.storage.SampleStorage;
import org.junit.jupiter.api.Test;

import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.*;

public class TestSampleProfiler {

    @Test
    public void testStartAndStop() throws InterruptedException {
        SampleStorage<FrequencyThreadStorage> storage = SampleStorage.frequency();
        SampleProfiler profiler = new SampleProfiler(storage);
        profiler.sleepTime = 5;

        sleep(500);

        // Nothing until the profiler starts
        assertNull(storage.getThreadStorage(Thread.currentThread().getId()));

        profiler.start();
        sleep(500);
        profiler.stop();

        FrequencyThreadStorage threadStorage = storage.getThreadStorage(Thread.currentThread().getId());

        // The number of samples depends on the OS scheduler, but at least 5 sounds reasonable to me
        long numSamples = threadStorage.rootNode.counter.get();
        System.out.println("numSamples is " + numSamples);
        assertTrue(numSamples >= 5);
        assertTrue(numSamples <= 125);
    }
}
