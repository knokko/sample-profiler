package com.github.knokko.profiler.storage;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class FrequencyThreadStorage implements ThreadStorage {

    public final LineNode rootNode = new LineNode();

    @Override
    public void insert(StackTraceElement[] stackTrace, long timestamp) {
        LineNode parentLineNode = rootNode;
        parentLineNode.counter.incrementAndGet();

        for (int index = stackTrace.length - 1; index >= 0; index--) {
            StackTraceElement child = stackTrace[index];
            MethodName childName = new MethodName(child.getClassName(), child.getMethodName());

            parentLineNode.children.computeIfAbsent(childName, name -> new MethodNode());
            MethodNode childMethodNode = parentLineNode.children.get(childName);
            childMethodNode.counter.incrementAndGet();

            childMethodNode.lines.computeIfAbsent(child.getLineNumber(), line -> new LineNode());
            parentLineNode = childMethodNode.lines.get(child.getLineNumber());
            parentLineNode.counter.incrementAndGet();
        }
    }

    public void print(PrintWriter output, int maxDepth, double thresholdPercentage) {
        long total = rootNode.counter.get();
        rootNode.print(output, "", total, maxDepth, (long) (total * thresholdPercentage * 0.01));
        output.flush();
    }

    public void print(PrintStream output, int maxDepth, double thresholdPercentage) {
        print(new PrintWriter(output), maxDepth, thresholdPercentage);
    }

    public static class MethodName {

        public final String className, methodName;

        public MethodName(String className, String methodName) {
            this.className = className;
            this.methodName = methodName;
        }

        public MethodName(StackTraceElement element) {
            this(element.getClassName(), element.getMethodName());
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(className) + 13 * Objects.hashCode(methodName);
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof MethodName) {
                MethodName otherName = (MethodName) other;
                return Objects.equals(this.className, otherName.className) && Objects.equals(this.methodName, otherName.methodName);
            } else return false;
        }
    }

    public static class MethodNode {

        public final AtomicLong counter = new AtomicLong(0);
        public final ConcurrentMap<Integer, LineNode> lines = new ConcurrentHashMap<>();
    }

    public static class LineNode {

        public final AtomicLong counter = new AtomicLong(0);
        public final ConcurrentMap<MethodName, MethodNode> children = new ConcurrentHashMap<>();

        void print(PrintWriter output, String prefix, long total, int maxDepth, long threshold) {
            long lineCounter = this.counter.get();
            output.printf("%s%d samples (%.2f%%)\n", prefix, lineCounter, 100.0 * lineCounter / total);
            for (Map.Entry<MethodName, MethodNode> child : children.entrySet().stream().sorted(
                    Comparator.comparingLong(entry -> -entry.getValue().counter.get())
            ).collect(Collectors.toList())) {
                MethodName methodName = child.getKey();
                MethodNode node = child.getValue();
                long nodeCounter = node.counter.get();
                if (nodeCounter < threshold) continue;

                output.printf(
                        "%s  %d samples (%.2f%%) %s.%s:\n", prefix, nodeCounter,
                        100.0 * nodeCounter / total, methodName.className, methodName.methodName
                );
                for (Map.Entry<Integer, LineNode> line : node.lines.entrySet().stream().sorted(
                        Comparator.comparingLong(entry -> -entry.getValue().counter.get())
                ).collect(Collectors.toList())) {
                    if (line.getValue().counter.get() < threshold || maxDepth <= 1) continue;

                    output.printf("%s  line %d:\n", prefix, line.getKey());
                    line.getValue().print(output, prefix + "    ", total, maxDepth - 1, threshold);
                }
            }
        }
    }
}
