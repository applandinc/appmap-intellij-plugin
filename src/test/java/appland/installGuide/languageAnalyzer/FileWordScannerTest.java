package appland.installGuide.languageAnalyzer;

import appland.AppMapBaseTest;
import org.junit.Test;

public class FileWordScannerTest extends AppMapBaseTest {
    @Test
    public void simple() {
        var file = myFixture.configureByText("a.txt", "a b c\nspring web framework");
        var scanner = new FileWordScanner(file.getVirtualFile());
        assertTrue(scanner.containsWord("a"));
        assertTrue(scanner.containsWord("b"));
        assertTrue(scanner.containsWord("c"));

        assertTrue(scanner.containsWord("spring"));
        assertTrue(scanner.containsWord("Spring"));
        assertTrue(scanner.containsWord("SPRING"));

        assertTrue(scanner.containsWord("web"));
        assertTrue(scanner.containsWord("framework"));

        assertFalse(scanner.containsWord("z"));
        assertFalse(scanner.containsWord("TEST"));
    }
}