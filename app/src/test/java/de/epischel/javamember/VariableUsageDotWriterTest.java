package de.epischel.javamember;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VariableUsageDotWriterTest {

    @Test
    void createsGraphForVariablesAndMethodsWithUsageAndCallEdges() {
        CompilationUnit cu = StaticJavaParser.parse("""
                class Sample {
                    private int a;
                    private int b;
                    private int c;

                    void useBoth() { a++; b++; helper(1); }
                    void helper(int value) { a += value; }
                    void useA() { a++; }
                    void unused() {}
                }
                """);

        String expected = """
                digraph member_usage {
                  rankdir=LR;
                  "variable:a" [label="a", shape=ellipse];
                  "variable:b" [label="b", shape=ellipse];
                  "variable:c" [label="c", shape=ellipse];
                  "method:useBoth()" [label="useBoth()", shape=box];
                  "method:helper(int)" [label="helper(int)", shape=box];
                  "method:useA()" [label="useA()", shape=box];
                  "method:unused()" [label="unused()", shape=box];
                  "method:useBoth()" -> "variable:a" [label="uses"];
                  "method:useBoth()" -> "variable:b" [label="uses"];
                  "method:helper(int)" -> "variable:a" [label="uses"];
                  "method:useA()" -> "variable:a" [label="uses"];
                  "method:useBoth()" -> "method:helper(int)" [label="calls"];
                }
                """;

        assertEquals(expected, VariableUsageDotWriter.toDot(cu));
    }

    @Test
    void excludesVariablesFromGraph() {
        CompilationUnit cu = StaticJavaParser.parse("""
                class Sample {
                    private int a;
                    private int b;
                    private int c;
                    void useAll() { a++; b++; c++; }
                }
                """);

        String expected = """
                digraph member_usage {
                  rankdir=LR;
                  "variable:a" [label="a", shape=ellipse];
                  "variable:c" [label="c", shape=ellipse];
                  "method:useAll()" [label="useAll()", shape=box];
                  "method:useAll()" -> "variable:a" [label="uses"];
                  "method:useAll()" -> "variable:c" [label="uses"];
                }
                """;

        assertEquals(expected, VariableUsageDotWriter.toDot(cu, Set.of("b")));
    }
}
