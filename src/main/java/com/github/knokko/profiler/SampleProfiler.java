package com.github.knokko.profiler;

import com.github.knokko.profiler.storage.SampleStorage;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Predicate;

public class SampleProfiler {

    private String threadName = "SampleProfiler";
    private boolean hasStarted = false;

    /**
     * When `sleepTime` > 0, the profiler will sleep for `sleepTime` milliseconds after taking each sample.
     * <ul>
     *     <li>Using a longer `sleepTime` decreases the profiling precision, but also reduces the system load.</li>
     *     <li>
     *          Using a `sleepTime` of 0 will cause the profiling thread to constantly watch all other threads.
     *          While this will give very accurate results, it may hurt the application performance.
     *     </li>
     * </ul>
     * The `sleepTime` can be changed at any point in time and will take effect as soon as possible.<br>
     * <b>
     *     Note: the OS scheduling policies can cause the actual sleep time to be larger than `sleepTime` ms. In my
     *     experience, it is accurate on Linux (sleeping 1 ms usually takes about 1.1 ms), but not on Windows
     *     (sleeping 1 ms can take anywhere between 2 and 8 ms).
     * </b>
     */
    public volatile int sleepTime = 1;

    /**
     * Setting `isPaused` to `true` will cause the profiler to pause as soon as possible. While paused, the profiler
     * won't take any samples. Instead, it will keep sleeping `sleepTime` ms until `isPaused` is `false` again.
     */
    public volatile boolean isPaused = false;
    private volatile boolean shouldStop = false;
    /**
     * All samples will be inserted into this `storage`. If you change this while the profiler is running, all new
     * samples will be inserted into the new storage.
     */
    public volatile SampleStorage<?> storage;
    /**
     * Only threads that satisfy this predicate will be profiled. You can change this filter whenever you want. Note:
     * the profiler will always skip its own profiling thread, regardless of the filter (because the stacktrace at
     * the moment of sampling is always the same).
     */
    public volatile Predicate<Thread> threadFilter = thread -> true;
    /**
     * Only stacktraces where the <b>full name</b> (including the package) of at least 1 class in the stacktrace
     * satisfies `classNameFilter` will be kept: all others stacktraces will be ignored. You can use this to filter out
     * IDE threads or other threads that you don't care about.
     */
    public volatile Predicate<String> classNameFilter = className -> true;

    private Thread sampleThread;

    public SampleProfiler(SampleStorage<?> storage) {
        this.storage = storage;
    }

    public void setThreadName(String threadName) {
        if (hasStarted) throw new IllegalStateException("Can't change thread name after the profiler has started");
        this.threadName = threadName;
    }

    public void start() {
        if (hasStarted) throw new IllegalStateException("Can't start twice");
        hasStarted = true;

        sampleThread = new Thread(this::startSampling);
        sampleThread.setDaemon(true);
        sampleThread.setName(threadName);
        sampleThread.start();
    }

    /**
     * Causes the profiler to stop (possibly after taking 1 more sample) and blocks the current thread until the
     * profiler is really stopped.
     */
    public void stop() {
        if (!hasStarted) throw new IllegalStateException("Can't stop before starting");
        if (shouldStop) throw new IllegalStateException("Can't stop twice");
        shouldStop = true;
        try {
            sampleThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void startSampling() {
        while (!shouldStop) {
            if (!isPaused) takeSample();

            long currentSleepTime = sleepTime;
            if (currentSleepTime > 0) {
                try {
                    //noinspection BusyWait
                    Thread.sleep(currentSleepTime);
                } catch (InterruptedException ignore) {
                    // When interrupted, just continue with the next iteration
                }
            }
        }
    }

    private void takeSample() {
        long timestamp = System.nanoTime();
        SampleStorage<?> currentStorage = storage;
        for (Map.Entry<Thread, StackTraceElement[]> dump : Thread.getAllStackTraces().entrySet()) {

            // Skip this thread because the sample result is guaranteed to be Thread.getAllStackTraces
            if (dump.getKey() == Thread.currentThread()) continue;

            if (!threadFilter.test(dump.getKey())) continue;

            if (Arrays.stream(dump.getValue()).noneMatch(element -> classNameFilter.test(element.getClassName()))) continue;

            currentStorage.insert(dump.getKey().getId(), dump.getValue(), timestamp);
        }
    }
}
