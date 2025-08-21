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
        List<String> variables = MemberVariableExtractor.getMemberVariableNames(cu);
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
}

