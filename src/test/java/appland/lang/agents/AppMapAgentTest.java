package appland.lang.agents;

import appland.AppMapBaseTest;
import org.junit.Test;

public class AppMapAgentTest extends AppMapBaseTest {
    @Test
    public void findByLanguage() {
        assertNotNull(AppMapAgent.findByLanguage(AgentLanguage.Ruby));
    }
}