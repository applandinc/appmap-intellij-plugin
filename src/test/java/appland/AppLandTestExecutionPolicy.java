package appland;

import com.intellij.testFramework.fixtures.IdeaTestExecutionPolicy;

/**
 * Test execution policy used by build.gradle.kts.
 */
@SuppressWarnings("unused")
public class AppLandTestExecutionPolicy extends IdeaTestExecutionPolicy {
    @Override
    protected String getName() {
        return "AppLand";
    }

    @Override
    public String getHomePath() {
        return System.getProperty("appland.testDataPath");
    }
}
