package de.epischel.javamember;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;

import java.io.IOException;
import java.nio.file.Path;

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
        System.out.println(cu.getPrimaryTypeName().orElse("Unknown"));
    }
}
