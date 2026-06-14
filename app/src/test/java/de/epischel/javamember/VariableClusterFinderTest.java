package de.epischel.javamember;

import com.github.javaparser.ast.CompilationUnit;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VariableClusterFinderTest {

    @Test
    void findsClustersOfVariables() throws Exception {
        String source = """
                class Sample {
                    private int a;
                    private int b;
                    private int c;
                    private int d;

                    void useAB() { a++; b++; }
                    void useBC() { b++; c++; }
                    void useD() { d++; }
                }
                """;
        Path temp = Files.createTempFile("Sample", ".java");
        Files.writeString(temp, source);
        CompilationUnit cu = App.parseFile(temp);
        List<Set<String>> clusters = VariableClusterFinder.findClusters(cu);
        assertEquals(List.of(Set.of("a", "b", "c"), Set.of("d")), clusters);
    }

    @Test
    void excludesVariablesBeforeBuildingClusters() throws Exception {
        String source = """
                class Sample {
                    private int a;
                    private int b;
                    private int c;

                    void useAB() { a++; b++; }
                    void useBC() { b++; c++; }
                }
                """;
        Path temp = Files.createTempFile("Sample", ".java");
        Files.writeString(temp, source);
        CompilationUnit cu = App.parseFile(temp);

        List<Set<String>> clusters = VariableClusterFinder.findClusters(cu, Set.of("b"));

        assertEquals(List.of(Set.of("a"), Set.of("c")), clusters);
    }

    @Test
    void findsOverlappingClustersAsMaximalCliques() throws Exception {
        String source = """
                class Sample {
                    private int a;
                    private int b;
                    private int c;
                    private int d;

                    void useAB() { a++; b++; }
                    void useBC() { b++; c++; }
                    void useD() { d++; }
                }
                """;
        Path temp = Files.createTempFile("Sample", ".java");
        Files.writeString(temp, source);
        CompilationUnit cu = App.parseFile(temp);

        List<Set<String>> clusters = VariableClusterFinder.findOverlappingClusters(cu);

        assertEquals(
                List.of(
                        new java.util.LinkedHashSet<>(List.of("a", "b")),
                        new java.util.LinkedHashSet<>(List.of("b", "c")),
                        Set.of("d")),
                clusters);
    }

    @Test
    void keepsFullyConnectedVariablesInOneOverlappingCluster() throws Exception {
        String source = """
                class Sample {
                    private int a;
                    private int b;
                    private int c;

                    void useAll() { a++; b++; c++; }
                }
                """;
        Path temp = Files.createTempFile("Sample", ".java");
        Files.writeString(temp, source);
        CompilationUnit cu = App.parseFile(temp);

        List<Set<String>> clusters = VariableClusterFinder.findOverlappingClusters(cu);

        assertEquals(List.of(Set.of("a", "b", "c")), clusters);
    }
}

