package de.epischel.javamember;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Writes a Graphviz graph containing member variables, methods, variable usage,
 * and internal method calls.
 */
public final class VariableUsageDotWriter {

    private VariableUsageDotWriter() {
        // utility class
    }

    public static void write(CompilationUnit cu, Path output) throws IOException {
        write(cu, output, Set.of());
    }

    public static void write(CompilationUnit cu, Path output, Set<String> excludedVariables) throws IOException {
        Files.writeString(output, toDot(cu, excludedVariables), StandardCharsets.UTF_8);
    }

    public static String toDot(CompilationUnit cu) {
        return toDot(cu, Set.of());
    }

    public static String toDot(CompilationUnit cu, Set<String> excludedVariables) {
        List<String> variables = MemberVariableExtractor.getMemberVariableNames(cu).stream()
                .filter(variable -> !excludedVariables.contains(variable))
                .toList();
        ClassOrInterfaceDeclaration clazz = cu.findFirst(ClassOrInterfaceDeclaration.class).orElse(null);
        List<MethodDeclaration> methods = clazz == null ? List.of() : clazz.getMethods();

        StringBuilder dot = new StringBuilder("digraph member_usage {\n");
        dot.append("  rankdir=LR;\n");
        for (String variable : variables) {
            dot.append("  \"").append(variableId(variable))
                    .append("\" [label=\"").append(escape(variable))
                    .append("\", shape=ellipse];\n");
        }
        for (MethodDeclaration method : methods) {
            dot.append("  \"").append(methodId(method))
                    .append("\" [label=\"").append(escape(method.getSignature().asString()))
                    .append("\", shape=box];\n");
        }

        for (MethodDeclaration method : methods) {
            for (String variable : variables) {
                if (MemberUsageFinder.directlyUsesVariable(method, variable)) {
                    dot.append("  \"").append(methodId(method))
                            .append("\" -> \"").append(variableId(variable))
                            .append("\" [label=\"uses\"];\n");
                }
            }
        }

        if (clazz != null) {
            for (MethodDeclaration caller : methods) {
                Set<MethodDeclaration> callees = new LinkedHashSet<>();
                for (MethodCallExpr call : caller.findAll(MethodCallExpr.class)) {
                    MemberUsageFinder.resolveCall(call, clazz, methods).ifPresent(callees::add);
                }
                for (MethodDeclaration callee : callees) {
                    dot.append("  \"").append(methodId(caller))
                            .append("\" -> \"").append(methodId(callee))
                            .append("\" [label=\"calls\"];\n");
                }
            }
        }
        return dot.append("}\n").toString();
    }

    private static String variableId(String variable) {
        return "variable:" + escape(variable);
    }

    private static String methodId(MethodDeclaration method) {
        return "method:" + escape(method.getSignature().asString());
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
