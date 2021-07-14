package appland.lang.agents;

import appland.AppMapBaseTest;
import org.junit.Test;

public class AppMapRubyAgentTest extends AppMapBaseTest {
    @Test
    public void updateEmptyFile() {
        var content = "";
        var updated = AppMapRubyAgent.updateGemDependency(content);
        assertEquals("\n\ngem 'appmap', :groups => [:development, :test]", updated);
    }

    @Test
    public void addGemDependency() {
        var content = "";
        var updated = AppMapRubyAgent.updateGemDependency(content);
        assertEquals("\n\ngem 'appmap', :groups => [:development, :test]", updated);
    }
}