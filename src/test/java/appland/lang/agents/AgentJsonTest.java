package appland.lang.agents;

import appland.AppMapBaseTest;
import org.junit.Test;

public class AgentJsonTest extends AppMapBaseTest {
    @Test
    public void parseConfiguration() {
        // language=JSON
        var json = "{\n" +
                "  \"configuration\": {\n" +
                "    \"filename\": \"appmap.yml\",\n" +
                "    \"contents\": \"---\\nname: sample_app_6th_ed\\npackages:\\n- path: app/controllers\\n- path: app/models\\n- path: lib\\n\"\n" +
                "  }\n" +
                "}";

        var response = AgentJson.parseInitResponse(json);
        assertNotNull(response);
        assertEquals("appmap.yml", response.filename);
        assertEquals("---\nname: sample_app_6th_ed\npackages:\n- path: app/controllers\n- path: app/models\n- path: lib\n", response.contents);
    }
}