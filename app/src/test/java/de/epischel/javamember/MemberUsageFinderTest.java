package de.epischel.javamember;

import com.github.javaparser.ast.CompilationUnit;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MemberUsageFinderTest {

    @Test
    void findsMethodsUsingVariable() throws Exception {
        String source = """
                class Sample {
                    private int a;
                    private int b;

                    void foo() { a = 1; }
                    void bar(int a) { this.a = a; }
                    void bar() { a = 2; }
                    void baz() { int a = 0; }
                    void useOther(Sample other) { other.a++; }
                    void callFoo() { foo(); }
                    void callCallFoo() { callFoo(); }
                    void qux() { b++; }
                    void quux(int b) { this.b = b; }
                    void callQux() { qux(); }
                    void noUse() {}
                }
                """;
        Path temp = Files.createTempFile("Sample", ".java");
        Files.writeString(temp, source);
        CompilationUnit cu = App.parseFile(temp);
        List<String> aUsage = MemberUsageFinder.findUsage(cu, "a").stream()
                .map(m -> m.getSignature().asString())
                .collect(Collectors.toList());
        List<String> bUsage = MemberUsageFinder.findUsage(cu, "b").stream()
                .map(m -> m.getSignature().asString())
                .collect(Collectors.toList());
        List<String> cUsage = MemberUsageFinder.findUsage(cu, "c").stream()
                .map(m -> m.getSignature().asString())
                .collect(Collectors.toList());

        assertEquals(List.of("foo()", "bar(int)", "bar()", "callFoo()", "callCallFoo()"), aUsage);
        assertEquals(List.of("qux()", "quux(int)", "callQux()"), bUsage);
        assertEquals(List.of(), cUsage);
    }
}

