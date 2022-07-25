package appland.installGuide.analyzer;

import appland.AppMapBaseTest;
import org.junit.Assert;
import org.junit.Test;

public class NodeVersionTest extends AppMapBaseTest {
    @Test
    public void parse() {
        Assert.assertEquals(new NodeVersion(10, 0, 0), NodeVersion.parse("10"));

        Assert.assertEquals(new NodeVersion(10, 20, 0), NodeVersion.parse("10.20"));

        Assert.assertEquals(new NodeVersion(10, 20, 30), NodeVersion.parse("10.20.30"));

        Assert.assertNull(NodeVersion.parse(""));
        Assert.assertNull(NodeVersion.parse("abc"));

        // unclear, but matches with VSCode implementation
        Assert.assertEquals(new NodeVersion(1, 0, 0), NodeVersion.parse("1.a.b"));
    }
}