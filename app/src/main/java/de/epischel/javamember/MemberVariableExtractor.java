package de.epischel.javamember;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility to extract all member variable names from a parsed Java class.
 */
public class MemberVariableExtractor {

    private MemberVariableExtractor() {
        // utility class
    }

    /**
     * Returns the names of all member variables declared in the given compilation unit.
     *
     * @param cu parsed compilation unit of a Java source file
     * @return list of member variable names in declaration order
     */
    public static List<String> getMemberVariableNames(CompilationUnit cu) {
        return cu.findAll(FieldDeclaration.class).stream()
                .flatMap(fd -> fd.getVariables().stream())
                .map(VariableDeclarator::getNameAsString)
                .collect(Collectors.toList());
    }
}

