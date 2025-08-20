package de.epischel.javamember;

import com.github.javaparser.ast.CompilationUnit;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AppTest {
    @Test
    void parsesProvidedFile() throws Exception {
        Path temp = Files.createTempFile("Sample", ".java");
        Files.writeString(temp, "class Sample {}");
        CompilationUnit cu = App.parseFile(temp);
        assertTrue(cu.getClassByName("Sample").isPresent());
    }
}
