# Sample profiler
## Profile your application by frequently taking samples (thread dumps)
The `SampleProvider` takes frequent samples (thread dumps) to profile
the performance of the application that uses it. The application can
start, stop, and pause the profiler whenever it wants, and read the 
results (or just print them for later manual inspection).

### Simple usage
```java
var storage = SampleStorage.frequency(); // Focus on the frequency of stacktraces
var profiler = new SampleProfiler(storage);

profiler.start(); // Start the profiling thread

// Do something that you want to profile

int maxDepth = 5; // Show at most 5 entries per stacktrace
double thresholdPercentage = 1.0; // Only show entries with frequency >= 1%

profiler.stop(); // Stop the profiler (optional)
var currentThreadResults = storage.getThreadStorage(Thread.currentThread.id());
currentThreadStorage.print(System.out, maxDepth, thresholdPercentage);
```

### Storage
Samples are stored on a per-thread basis. The profiler will insert 
the data it collects into a `SampleStorage`, which will propagate
the data to the `ThreadStorage` for the corresponding thread. You
can create a `SampleStorage` instance using 
`SampleStorage.frequency()` or `SampleStorage.timeline()`. There
are 2 types of `ThreadStorage`: `FrequencyThreadStorage` and
`TimelineThreadStorage`:

#### Frequency
A `FrequencyThreadStorage` maintains a tree structure that keeps
track of how often each sampled stacktrace was inserted. The
`rootNode` is intentionally `public`, which allows the application
to inspect the tree live. Alternatively, you can call the `print`
method to print a simple overview of the tree for manual 
inspection. This type of storage shows which parts of your code
are frequently executed, but doesn't tell anything about when
it is executed.

Note: the tree is thread-safe, so you can inspect or print the
tree while the profiler is sampling it. However, the results may
not be entirely consistent if you do this (e.g. some nodes may
have a higher sample count than their parent, or even more than
100%). While this is fine for simple monitoring purposes, it
might be problematic for some use cases. To avoid inconsistent
results, you should pause or stop the profiler.

#### Timeline
A `TimelineThreadStorage` maintains a 'timeline' of samples
(a sorted set that is sorted by the timestamp). If the `sleepTime`
of the profiler is short enough (I recommend `0` or maybe `1`),
it can give an accurate impression of what each thread was doing
at any point in time. This is mostly useful for investigating the
latency of some task. This class provides the following methods to
query the samples:
- `getBetween(startTime, endTime)`: gets a list of all samples 
that were taken between `startTime` and `endTime`.
- `getIntervalsBetween(startTime, endTime)`: gets a list of
*intervals* between `startTime` and `endTime`. An interval is
basically a group of consecutive samples with the same stacktrace:
its `startTime` is the timestamp of the first sample and the
`endTime` is the timestamp of the first sample of the next 
interval.
- `sample(startTime, endTime)`: gets the common 'ancestor'
stacktrace of all samples between `startTime` end `endTime`.

The `timestamp` of each sample is the return value of
`System.nanoTime()` at the time the sample was taken.
Unlike `FrequencyThreadStorage`, this class doesn't have a 
simple `print` method. Instead, you should use one of the methods
to get a list of samples, and just print those (which is simple
since this class doesn't expose a tree structure).

### Add to your build
This library requires Java 8 or later (and it's tested against
Java 8, 11, 17, and 20).
#### Gradle
```
...
repositories {
  ...
  maven { url 'https://jitpack.io' }
}
...
dependencies {
  ...
  implementation 'com.github.knokko:sample-profiler:v1.0.0'
}
```

#### Maven
```
...
<repositories>
  ...
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>
...
<dependency>
  <groupId>com.github.knokko</groupId>
  <artifactId>sample-profiler</artifactId>
  <version>v1.0.0</version>
</dependency>
```
