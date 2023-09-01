package com.github.knokko.profiler.storage;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class TestFrequencyThreadStorage {

    @Test
    public void testSimple() {
        FrequencyThreadStorage storage = new FrequencyThreadStorage();

        StackTraceElement dummy1 = new StackTraceElement("test.dummy.Dummy", "doNothing", "Dummy", 41);
        StackTraceElement dummy2 = new StackTraceElement("test.dummy.Dummy", "alsoDoesNothing", "Dummy", 20);

        for (int counter = 0; counter < 10; counter++) {
            storage.insert(new StackTraceElement[]{ dummy1 }, counter);
        }
        for (int counter = 0; counter < 5; counter++) {
            storage.insert(new StackTraceElement[]{ dummy2 }, counter);
        }

        assertEquals(2, storage.rootNode.children.size());
        assertEquals(15, storage.rootNode.counter.get());
        FrequencyThreadStorage.MethodNode dummyNode1 = storage.rootNode.children.get(new FrequencyThreadStorage.MethodName(dummy1));
        assertEquals(10, dummyNode1.counter.get());
        assertEquals(1, dummyNode1.lines.size());
        assertEquals(10, dummyNode1.lines.get(41).counter.get());
        assertEquals(0, dummyNode1.lines.get(41).children.size());

        FrequencyThreadStorage.MethodNode dummyNode2 = storage.rootNode.children.get(new FrequencyThreadStorage.MethodName(dummy2));
        assertEquals(5, dummyNode2.counter.get());
        assertEquals(1, dummyNode2.lines.size());
        assertEquals(5, dummyNode2.lines.get(20).counter.get());
        assertEquals(0, dummyNode2.lines.get(20).children.size());

        StackTraceElement element11 = new StackTraceElement(
                "work.stuff.Master", "startWork", "Master", 5
        );
        StackTraceElement element12 = new StackTraceElement(
                "work.stuff.Master", "startWork", "Master", 6
        );
        StackTraceElement element111 = new StackTraceElement(
                "work.stuff.Worker", "shipResources", "Worker", 50
        );
        StackTraceElement element112 = new StackTraceElement(
                "work.stuff.Worker", "doActualWork", "Worker", 80
        );
        StackTraceElement element1121 = new StackTraceElement(
                "work.stuff.Worker", "detailedWork", "Worker", 300
        );
        StackTraceElement element121 = new StackTraceElement(
                "work.stuff.Worker", "finishUp", "Worker", 20
        );

        StackTraceElement[] trace111 = { element111, element11 };
        StackTraceElement[] trace112 = { element112, element11 };
        StackTraceElement[] trace1121 = { element1121, element112, element11 };
        StackTraceElement[] trace12 = { element12 };
        StackTraceElement[] trace121 = { element121, element12 };

        for (int counter = 0; counter < 5; counter++) storage.insert(trace112, counter);
        for (int counter = 0; counter < 10; counter++) storage.insert(trace121, counter);
        storage.insert(trace12, 12);
        storage.insert(trace111, 111);
        storage.insert(trace1121, 1121);
        for (int counter = 0; counter < 10; counter++) storage.insert(trace121, counter);

        FrequencyThreadStorage.MethodNode node1 = storage.rootNode.children.get(new FrequencyThreadStorage.MethodName(element11));
        assertEquals(28, node1.counter.get());
        assertEquals(2, node1.lines.size());

        FrequencyThreadStorage.LineNode node11 = node1.lines.get(5);
        assertEquals(7, node11.counter.get());
        assertEquals(2, node11.children.size());

        FrequencyThreadStorage.LineNode node12 = node1.lines.get(6);
        assertEquals(21, node12.counter.get());
        assertEquals(1, node12.children.size());

        FrequencyThreadStorage.MethodNode node111 = node11.children.get(new FrequencyThreadStorage.MethodName(element111));
        assertEquals(1, node111.counter.get());
        assertEquals(1, node111.lines.size());
        FrequencyThreadStorage.LineNode line111 = node111.lines.get(50);
        assertEquals(1, line111.counter.get());
        assertEquals(0, line111.children.size());

        FrequencyThreadStorage.MethodNode node112 = node11.children.get(new FrequencyThreadStorage.MethodName(element112));
        assertEquals(6, node112.counter.get());
        assertEquals(1, node112.lines.size());
        FrequencyThreadStorage.LineNode line112 = node112.lines.get(80);
        assertEquals(6, line112.counter.get());
        assertEquals(1, line112.children.size());

        FrequencyThreadStorage.MethodNode node121 = node12.children.get(new FrequencyThreadStorage.MethodName(element121));
        assertEquals(20, node121.counter.get());
        assertEquals(1, node121.lines.size());
        FrequencyThreadStorage.LineNode line121 = node121.lines.get(20);
        assertEquals(20, line121.counter.get());
        assertEquals(0, line121.children.size());

        FrequencyThreadStorage.MethodNode node1121 = line112.children.get(new FrequencyThreadStorage.MethodName(element1121));
        assertEquals(1, node1121.counter.get());
        assertEquals(1, node1121.lines.size());
        FrequencyThreadStorage.LineNode line1121 = node1121.lines.get(300);
        assertEquals(1, line1121.counter.get());
        assertEquals(0, line1121.children.size());

        ByteArrayOutputStream captureOutput = new ByteArrayOutputStream();
        storage.print(new PrintWriter(captureOutput), 10, 1.0);

        Scanner outputReader = new Scanner(new ByteArrayInputStream(captureOutput.toByteArray()));
        assertEquals("43 samples (100.00%)", outputReader.nextLine());
        assertEquals("  28 samples (65.12%) work.stuff.Master.startWork:", outputReader.nextLine());
        assertEquals("  line 6:", outputReader.nextLine());
        assertEquals("    21 samples (48.84%)", outputReader.nextLine());
        assertEquals("      20 samples (46.51%) work.stuff.Worker.finishUp:", outputReader.nextLine());
        assertEquals("      line 20:", outputReader.nextLine());
        assertEquals("        20 samples (46.51%)", outputReader.nextLine());
        assertEquals("  line 5:", outputReader.nextLine());
        assertEquals("    7 samples (16.28%)", outputReader.nextLine());
        assertEquals("      6 samples (13.95%) work.stuff.Worker.doActualWork:", outputReader.nextLine());
        assertEquals("      line 80:", outputReader.nextLine());
        assertEquals("        6 samples (13.95%)", outputReader.nextLine());
        assertEquals("          1 samples (2.33%) work.stuff.Worker.detailedWork:", outputReader.nextLine());
        assertEquals("          line 300:", outputReader.nextLine());
        assertEquals("            1 samples (2.33%)", outputReader.nextLine());
        assertEquals("      1 samples (2.33%) work.stuff.Worker.shipResources:", outputReader.nextLine());
        assertEquals("      line 50:", outputReader.nextLine());
        assertEquals("        1 samples (2.33%)", outputReader.nextLine());
        assertEquals("  10 samples (23.26%) test.dummy.Dummy.doNothing:", outputReader.nextLine());
        assertEquals("  line 41:", outputReader.nextLine());
        assertEquals("    10 samples (23.26%)", outputReader.nextLine());
        assertEquals("  5 samples (11.63%) test.dummy.Dummy.alsoDoesNothing:", outputReader.nextLine());
        assertEquals("  line 20:", outputReader.nextLine());
        assertEquals("    5 samples (11.63%)", outputReader.nextLine());
        assertFalse(outputReader.hasNextLine());
    }
}
