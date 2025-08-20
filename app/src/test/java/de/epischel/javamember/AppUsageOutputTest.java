package de.epischel.javamember;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppUsageOutputTest {

    @Test
    void printsVariableUsagePerMember() throws Exception {
        String source = """
                class Sample {
                    private int a;
                    private int b;
                    private int c;

                    void foo() { a = 1; }
                    void bar(int a) { this.a = a; }
                    void bar() { a = 2; }
                    void qux() { b++; }
                    void quux(int b) { this.b = b; }
                    void noUse() {}
                }
                """;

        Path temp = Files.createTempFile("Sample", ".java");
        Files.writeString(temp, source);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        try {
            System.setOut(new PrintStream(out));
            App.main(new String[]{temp.toString()});
        } finally {
            System.setOut(originalOut);
        }

        String expected = String.join(System.lineSeparator(),
                "a: foo(), bar(int), bar()",
                "b: qux(), quux(int)",
                "c:") + System.lineSeparator();

        assertEquals(expected, out.toString());
    }
}

