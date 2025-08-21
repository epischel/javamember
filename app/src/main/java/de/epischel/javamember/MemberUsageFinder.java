package de.epischel.javamember;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility to find methods of a class that use a given member variable.
 */
public class MemberUsageFinder {

    private MemberUsageFinder() {
        // utility class
    }

    /**
     * Returns all methods in the primary class of the given compilation unit that
     * either access the specified member variable directly or invoke another method
     * (recursively) that does. The returned declarations allow callers to inspect
     * the full method signature, which is important when methods are overloaded.
     *
     * @param cu       parsed compilation unit of a Java source file
     * @param variable name of the member variable to search for
     * @return list of method declarations that eventually use the member variable
     */
    public static List<MethodDeclaration> findUsage(CompilationUnit cu, String variable) {
        return cu.findFirst(ClassOrInterfaceDeclaration.class)
                .map(clazz -> {
                    List<MethodDeclaration> methods = clazz.getMethods();
                    Map<MethodDeclaration, Set<MethodDeclaration>> callGraph = buildCallGraph(clazz);
                    Map<MethodDeclaration, Set<MethodDeclaration>> callers = invert(callGraph);

                    Set<MethodDeclaration> result = new HashSet<>();
                    Deque<MethodDeclaration> stack = new ArrayDeque<>();

                    for (MethodDeclaration m : methods) {
                        if (usesVariable(m, variable)) {
                            result.add(m);
                            stack.push(m);
                        }
                    }

                    while (!stack.isEmpty()) {
                        MethodDeclaration current = stack.pop();
                        for (MethodDeclaration caller : callers.getOrDefault(current, Set.of())) {
                            if (result.add(caller)) {
                                stack.push(caller);
                            }
                        }
                    }

                    return methods.stream()
                            .filter(result::contains)
                            .collect(Collectors.toList());
                })
                .orElse(List.of());
    }

    private static Map<MethodDeclaration, Set<MethodDeclaration>> buildCallGraph(ClassOrInterfaceDeclaration clazz) {
        List<MethodDeclaration> methods = clazz.getMethods();
        Map<MethodDeclaration, Set<MethodDeclaration>> graph = new HashMap<>();
        for (MethodDeclaration caller : methods) {
            Set<MethodDeclaration> callees = caller.findAll(MethodCallExpr.class).stream()
                    .map(call -> resolveCall(call, clazz, methods))
                    .flatMap(Optional::stream)
                    .collect(Collectors.toSet());
            graph.put(caller, callees);
        }
        return graph;
    }

    private static Map<MethodDeclaration, Set<MethodDeclaration>> invert(Map<MethodDeclaration, Set<MethodDeclaration>> graph) {
        Map<MethodDeclaration, Set<MethodDeclaration>> callers = new HashMap<>();
        for (Map.Entry<MethodDeclaration, Set<MethodDeclaration>> entry : graph.entrySet()) {
            MethodDeclaration caller = entry.getKey();
            for (MethodDeclaration callee : entry.getValue()) {
                callers.computeIfAbsent(callee, k -> new HashSet<>()).add(caller);
            }
        }
        return callers;
    }

    private static Optional<MethodDeclaration> resolveCall(MethodCallExpr call, ClassOrInterfaceDeclaration clazz, List<MethodDeclaration> methods) {
        if (call.getScope().isPresent()) {
            var scope = call.getScope().get();
            if (!(scope.isThisExpr() || scope.toString().equals(clazz.getNameAsString()))) {
                return Optional.empty();
            }
        }
        String name = call.getNameAsString();
        int argCount = call.getArguments().size();
        return methods.stream()
                .filter(m -> m.getNameAsString().equals(name) && m.getParameters().size() == argCount)
                .findFirst();
    }

    private static boolean usesVariable(MethodDeclaration method, String variable) {
        boolean hasLocal = method.getParameters().stream()
                .anyMatch(p -> p.getNameAsString().equals(variable))
                || method.findAll(VariableDeclarator.class).stream()
                .anyMatch(vd -> vd.getNameAsString().equals(variable));

        String className = method.findAncestor(ClassOrInterfaceDeclaration.class)
                .map(ClassOrInterfaceDeclaration::getNameAsString)
                .orElse("");

        boolean uses = method.findAll(FieldAccessExpr.class).stream()
                .anyMatch(fa -> fa.getNameAsString().equals(variable)
                        && (fa.getScope().isThisExpr()
                        || fa.getScope().toString().equals(className)));

        if (!hasLocal) {
            uses = uses || method.findAll(NameExpr.class).stream()
                    .anyMatch(ne -> ne.getNameAsString().equals(variable));
        }
        return uses;
    }
}

