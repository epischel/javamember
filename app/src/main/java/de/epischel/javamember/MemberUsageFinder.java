package de.epischel.javamember;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.NameExpr;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility to find methods of a class that use a given member variable.
 */
public class MemberUsageFinder {

    private MemberUsageFinder() {
        // utility class
    }

    /**
     * Returns all methods in the primary class of the given compilation unit
     * that reference the specified member variable. The returned declarations allow
     * callers to inspect the full method signature, which is important when methods
     * are overloaded.
     *
     * @param cu       parsed compilation unit of a Java source file
     * @param variable name of the member variable to search for
     * @return list of method declarations that use the member variable
     */
    public static List<MethodDeclaration> findUsage(CompilationUnit cu, String variable) {
        return cu.findFirst(ClassOrInterfaceDeclaration.class)
                .map(clazz -> clazz.getMethods().stream()
                        .filter(m -> usesVariable(m, variable))
                        .collect(Collectors.toList()))
                .orElse(List.of());
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

