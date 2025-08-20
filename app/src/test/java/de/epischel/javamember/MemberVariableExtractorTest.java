package de.epischel.javamember;

import com.github.javaparser.ast.CompilationUnit;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MemberVariableExtractorTest {

    @Test
    void extractsMemberVariables() throws Exception {
        String source = """
                class Sample {
                    private int a;
                    String b, c;
                    static final double PI = 3.14;
                    void method() { int local = 0; }
                }
                """;
        Path temp = Files.createTempFile("Sample", ".java");
        Files.writeString(temp, source);
        CompilationUnit cu = App.parseFile(temp);
        List<String> vars = MemberVariableExtractor.getMemberVariableNames(cu);
        assertEquals(List.of("a", "b", "c", "PI"), vars);
    }
}

