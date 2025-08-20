package de.epischel.javamember;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

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
            List<String> methods = MemberUsageFinder.findUsage(cu, variable).stream()
                    .map(m -> m.getSignature().asString())
                    .collect(Collectors.toList());
            String joined = String.join(", ", methods);
            System.out.println(variable + ":" + (joined.isEmpty() ? "" : " " + joined));
        }
    }
}
