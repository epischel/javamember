package de.epischel.javamember;

import com.github.javaparser.ast.CompilationUnit;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility to find clusters of member variables that are used together in methods.
 */
public class VariableClusterFinder {

    private VariableClusterFinder() {
        // utility class
    }

    /**
     * Finds clusters of variables that are used in at least one common method.
     *
     * @param cu parsed compilation unit of a Java source file
     * @return list of clusters, each represented as a set of variable names
     */
    public static List<Set<String>> findClusters(CompilationUnit cu) {
        return findClusters(cu, Set.of());
    }

    public static List<Set<String>> findClusters(CompilationUnit cu, Set<String> excludedVariables) {
        List<String> variables = MemberVariableExtractor.getMemberVariableNames(cu).stream()
                .filter(variable -> !excludedVariables.contains(variable))
                .toList();
        Map<String, Set<String>> graph = buildUsageGraph(cu, variables);

        List<Set<String>> clusters = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        for (String v : variables) {
            if (visited.contains(v)) {
                continue;
            }
            Set<String> cluster = new LinkedHashSet<>();
            Deque<String> stack = new ArrayDeque<>();
            stack.push(v);
            visited.add(v);
            while (!stack.isEmpty()) {
                String current = stack.pop();
                cluster.add(current);
                for (String neighbor : graph.get(current)) {
                    if (visited.add(neighbor)) {
                        stack.push(neighbor);
                    }
                }
            }
            clusters.add(cluster);
        }
        return clusters;
    }

    /**
     * Finds maximal groups whose variables are pairwise used together.
     *
     * <p>Unlike connected components, maximal cliques may overlap. For example,
     * usages {@code a-b} and {@code b-c} produce the clusters {@code {a,b}} and
     * {@code {b,c}} instead of merging all three variables.</p>
     *
     * @param cu parsed compilation unit of a Java source file
     * @return maximal, potentially overlapping clusters
     */
    public static List<Set<String>> findOverlappingClusters(CompilationUnit cu) {
        return findOverlappingClusters(cu, Set.of());
    }

    public static List<Set<String>> findOverlappingClusters(
            CompilationUnit cu,
            Set<String> excludedVariables) {
        List<String> variables = MemberVariableExtractor.getMemberVariableNames(cu).stream()
                .filter(variable -> !excludedVariables.contains(variable))
                .toList();
        Map<String, Set<String>> graph = buildUsageGraph(cu, variables);
        List<Set<String>> clusters = new ArrayList<>();

        findMaximalCliques(
                new LinkedHashSet<>(),
                new LinkedHashSet<>(variables),
                new LinkedHashSet<>(),
                graph,
                clusters);

        Map<String, Integer> declarationOrder = new HashMap<>();
        for (int i = 0; i < variables.size(); i++) {
            declarationOrder.put(variables.get(i), i);
        }
        clusters.sort((left, right) -> compareClusters(left, right, declarationOrder));
        return clusters;
    }

    private static Map<String, Set<String>> buildUsageGraph(
            CompilationUnit cu,
            List<String> variables) {
        Map<String, Set<String>> usage = new HashMap<>();
        for (String var : variables) {
            Set<String> methods = MemberUsageFinder.findUsage(cu, var).stream()
                    .map(m -> m.getSignature().asString())
                    .collect(Collectors.toSet());
            usage.put(var, methods);
        }

        Map<String, Set<String>> graph = new HashMap<>();
        for (String v : variables) {
            graph.put(v, new HashSet<>());
        }
        for (int i = 0; i < variables.size(); i++) {
            String vi = variables.get(i);
            for (int j = i + 1; j < variables.size(); j++) {
                String vj = variables.get(j);
                Set<String> methodsI = usage.get(vi);
                Set<String> methodsJ = usage.get(vj);
                Set<String> intersection = new HashSet<>(methodsI);
                intersection.retainAll(methodsJ);
                if (!intersection.isEmpty()) {
                    graph.get(vi).add(vj);
                    graph.get(vj).add(vi);
                }
            }
        }
        return graph;
    }

    private static void findMaximalCliques(
            LinkedHashSet<String> current,
            LinkedHashSet<String> candidates,
            LinkedHashSet<String> excluded,
            Map<String, Set<String>> graph,
            List<Set<String>> result) {
        if (candidates.isEmpty() && excluded.isEmpty()) {
            result.add(new LinkedHashSet<>(current));
            return;
        }

        for (String variable : List.copyOf(candidates)) {
            current.add(variable);
            findMaximalCliques(
                    current,
                    intersection(candidates, graph.get(variable)),
                    intersection(excluded, graph.get(variable)),
                    graph,
                    result);
            current.remove(variable);
            candidates.remove(variable);
            excluded.add(variable);
        }
    }

    private static LinkedHashSet<String> intersection(
            Set<String> variables,
            Set<String> neighbors) {
        return variables.stream()
                .filter(neighbors::contains)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static int compareClusters(
            Set<String> left,
            Set<String> right,
            Map<String, Integer> declarationOrder) {
        var leftIterator = left.iterator();
        var rightIterator = right.iterator();
        while (leftIterator.hasNext() && rightIterator.hasNext()) {
            int comparison = Integer.compare(
                    declarationOrder.get(leftIterator.next()),
                    declarationOrder.get(rightIterator.next()));
            if (comparison != 0) {
                return comparison;
            }
        }
        return Integer.compare(left.size(), right.size());
    }
}

