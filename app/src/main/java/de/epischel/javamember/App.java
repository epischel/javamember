package de.epischel.javamember;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class App {
    public static CompilationUnit parseFile(Path path) throws IOException {
        return StaticJavaParser.parse(path);
    }

    public static void main(String[] args) throws IOException {
        CliOptions options = parseArguments(args);
        if (options == null) {
            printUsage();
            return;
        }

        CompilationUnit cu = parseFile(options.source());
        Set<String> excludedVariables = new LinkedHashSet<>(options.excludedVariables());
        if (options.ignoreConstants()) {
            excludedVariables.addAll(MemberVariableExtractor.getConstantNames(cu));
        }
        if (options.dotOutput() != null) {
            VariableUsageDotWriter.write(cu, options.dotOutput(), excludedVariables);
            return;
        }

        List<String> variables = MemberVariableExtractor.getMemberVariableNames(cu).stream()
                .filter(variable -> !excludedVariables.contains(variable))
                .toList();
        for (String variable : variables) {
            System.out.println(variable + ":");
            MemberUsageFinder.findUsage(cu, variable).stream()
                    .map(m -> m.getSignature().asString())
                    .forEach(signature -> System.out.println("- " + signature));
        }
        System.out.println("");
        System.out.println("Cluster:");
        List<Set<String>> clusters = options.overlappingClusters()
                ? VariableClusterFinder.findOverlappingClusters(cu, excludedVariables)
                : VariableClusterFinder.findClusters(cu, excludedVariables);
        clusters.stream()
                .filter(cluster -> cluster.size() > 1)
                .forEach(cluster -> System.out.println(String.join(", ", cluster)));
    }

    private static CliOptions parseArguments(String[] args) {
        Path source = null;
        Path dotOutput = null;
        boolean ignoreConstants = false;
        boolean overlappingClusters = false;
        Set<String> excludedVariables = new LinkedHashSet<>();

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--dot" -> {
                    if (dotOutput != null || ++i >= args.length) {
                        return null;
                    }
                    dotOutput = Path.of(args[i]);
                }
                case "--exclude" -> {
                    if (++i >= args.length) {
                        return null;
                    }
                    for (String variable : args[i].split(",")) {
                        String trimmed = variable.trim();
                        if (!trimmed.isEmpty()) {
                            excludedVariables.add(trimmed);
                        }
                    }
                }
                case "--ignore-constants" -> ignoreConstants = true;
                case "--overlapping-clusters" -> overlappingClusters = true;
                default -> {
                    if (args[i].startsWith("--") || source != null) {
                        return null;
                    }
                    source = Path.of(args[i]);
                }
            }
        }

        return source == null
                ? null
                : new CliOptions(
                        source,
                        dotOutput,
                        Set.copyOf(excludedVariables),
                        ignoreConstants,
                        overlappingClusters);
    }

    private static void printUsage() {
        System.err.println("Usage:");
        System.err.println("  javamember [--exclude <name1,name2>] [--ignore-constants]"
                + " [--overlapping-clusters] <Java source file>");
        System.err.println("  javamember --dot <output.dot> [--exclude <name1,name2>]"
                + " [--ignore-constants] <Java source file>");
    }

    private record CliOptions(
            Path source,
            Path dotOutput,
            Set<String> excludedVariables,
            boolean ignoreConstants,
            boolean overlappingClusters) {
    }
}
