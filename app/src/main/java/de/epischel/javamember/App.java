package de.epischel.javamember;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class App {
    public static CompilationUnit parseFile(Path path) throws IOException {
        return StaticJavaParser.parse(path);
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.err.println("Please provide a path to a Java source file.");
            return;
        }
        Path file = Path.of(args[0]);
        CompilationUnit cu = parseFile(file);
        List<String> variables = MemberVariableExtractor.getMemberVariableNames(cu);
        for (String variable : variables) {
            System.out.println(variable + ":");
            MemberUsageFinder.findUsage(cu, variable).stream()
                    .map(m -> m.getSignature().asString())
                    .forEach(signature -> System.out.println("- " + signature));
        }
        System.out.println("");
        System.out.println("Cluster:");
        VariableClusterFinder.findClusters(cu).stream()
                .filter(cluster -> cluster.size() > 1)
                .forEach(cluster -> System.out.println(String.join(", ", cluster)));
    }
}
