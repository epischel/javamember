package de.epischel.javamember;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppUsageOutputTest {

    @Test
    void printsVariableUsagePerMember() throws Exception {
        String source = """
                class Sample {
                    private int a;
                    private int b;
                    private int c;

                    void foo() { a = 1; }
                    void bar(int a) { this.a = a; }
                    void bar() { a = 2; }
                    void callFoo() { foo(); }
                    void qux() { b++; }
                    void quux(int b) { this.b = b; }
                    void callQux() { qux(); }
                    void useBoth() { a++; b++; }
                    void noUse() {}
                }
                """;

        Path temp = Files.createTempFile("Sample", ".java");
        Files.writeString(temp, source);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        try {
            System.setOut(new PrintStream(out));
            App.main(new String[]{temp.toString()});
        } finally {
            System.setOut(originalOut);
        }

        String expected = String.join(System.lineSeparator(),
                "a:",
                "- foo()",
                "- bar(int)",
                "- bar()",
                "- callFoo()",
                "- useBoth()",
                "b:",
                "- qux()",
                "- quux(int)",
                "- callQux()",
                "- useBoth()",
                "c:",
                "",
                "Cluster:",
                "a, b") + System.lineSeparator();

        assertEquals(expected, out.toString());
    }

    @Test
    void writesDotGraphToRequestedFile() throws Exception {
        Path source = Files.createTempFile("Sample", ".java");
        Files.writeString(source, """
                class Sample {
                    private int a;
                    private int b;
                    void useBoth() { a++; b++; }
                }
                """);
        Path output = Files.createTempFile("variables", ".dot");

        App.main(new String[]{"--dot", output.toString(), source.toString()});

        String expected = """
                digraph member_usage {
                  rankdir=LR;
                  "variable:a" [label="a", shape=ellipse];
                  "variable:b" [label="b", shape=ellipse];
                  "method:useBoth()" [label="useBoth()", shape=box];
                  "method:useBoth()" -> "variable:a" [label="uses"];
                  "method:useBoth()" -> "variable:b" [label="uses"];
                }
                """;
        assertEquals(expected, Files.readString(output));
    }

    @Test
    void excludesCommaSeparatedVariablesFromTextOutputAndClusters() throws Exception {
        Path source = Files.createTempFile("Sample", ".java");
        Files.writeString(source, """
                class Sample {
                    private int a;
                    private int b;
                    private int c;
                    void useAB() { a++; b++; }
                    void useBC() { b++; c++; }
                }
                """);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        try {
            System.setOut(new PrintStream(out));
            App.main(new String[]{"--exclude", " b, c, ", source.toString()});
        } finally {
            System.setOut(originalOut);
        }

        String expected = String.join(System.lineSeparator(),
                "a:",
                "- useAB()",
                "",
                "Cluster:") + System.lineSeparator();
        assertEquals(expected, out.toString());
    }

    @Test
    void excludesCommaSeparatedVariablesFromDotOutput() throws Exception {
        Path source = Files.createTempFile("Sample", ".java");
        Files.writeString(source, """
                class Sample {
                    private int a;
                    private int b;
                    private int c;
                    void useAll() { a++; b++; c++; }
                }
                """);
        Path output = Files.createTempFile("variables", ".dot");

        App.main(new String[]{
                "--dot", output.toString(),
                "--exclude", "a, c",
                source.toString()
        });

        String expected = """
                digraph member_usage {
                  rankdir=LR;
                  "variable:b" [label="b", shape=ellipse];
                  "method:useAll()" [label="useAll()", shape=box];
                  "method:useAll()" -> "variable:b" [label="uses"];
                }
                """;
        assertEquals(expected, Files.readString(output));
    }

    @Test
    void ignoresConstantsInTextOutputAndClusters() throws Exception {
        Path source = Files.createTempFile("Sample", ".java");
        Files.writeString(source, """
                class Sample {
                    private static final int LIMIT = 10;
                    private final int initialValue = 1;
                    private int value;
                    void update() { value = LIMIT + initialValue; }
                }
                """);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        try {
            System.setOut(new PrintStream(out));
            App.main(new String[]{"--ignore-constants", source.toString()});
        } finally {
            System.setOut(originalOut);
        }

        String expected = String.join(System.lineSeparator(),
                "initialValue:",
                "- update()",
                "value:",
                "- update()",
                "",
                "Cluster:",
                "initialValue, value") + System.lineSeparator();
        assertEquals(expected, out.toString());
    }

    @Test
    void ignoresConstantsInDotOutput() throws Exception {
        Path source = Files.createTempFile("Sample", ".java");
        Files.writeString(source, """
                class Sample {
                    private static final int LIMIT = 10;
                    private int value;
                    void update() { value = LIMIT; }
                }
                """);
        Path output = Files.createTempFile("variables", ".dot");

        App.main(new String[]{
                "--dot", output.toString(),
                "--ignore-constants",
                source.toString()
        });

        String expected = """
                digraph member_usage {
                  rankdir=LR;
                  "variable:value" [label="value", shape=ellipse];
                  "method:update()" [label="update()", shape=box];
                  "method:update()" -> "variable:value" [label="uses"];
                }
                """;
        assertEquals(expected, Files.readString(output));
    }

    @Test
    void printsOverlappingClustersWhenRequested() throws Exception {
        Path source = Files.createTempFile("Sample", ".java");
        Files.writeString(source, """
                class Sample {
                    private int a;
                    private int b;
                    private int c;
                    void useAB() { a++; b++; }
                    void useBC() { b++; c++; }
                }
                """);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        try {
            System.setOut(new PrintStream(out));
            App.main(new String[]{"--overlapping-clusters", source.toString()});
        } finally {
            System.setOut(originalOut);
        }

        String expected = String.join(System.lineSeparator(),
                "a:",
                "- useAB()",
                "b:",
                "- useAB()",
                "- useBC()",
                "c:",
                "- useBC()",
                "",
                "Cluster:",
                "a, b",
                "b, c") + System.lineSeparator();
        assertEquals(expected, out.toString());
    }
}

