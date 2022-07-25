package appland.toolwindow.installGuide;

import appland.installGuide.InstallGuideViewPage;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.panels.VerticalLayout;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

import static appland.installGuide.InstallGuideEditorProvider.open;

/**
 * Manages a list of collapsible user milestone panels.
 */
public class InstallGuidePanel extends JPanel {
    public InstallGuidePanel(@NotNull Project project) {
        super(new VerticalLayout(5));

        addPanel("Quickstart", new InstallGuideContentPanel() {
            @Override
            protected void setupPanel() {
                for (var page : InstallGuideViewPage.values()) {
                    add(new StatusLabel(getStatus(page), page.getPageTitle(), () -> open(project, page)));
                }
            }
        });

        addPanel("Documentation", new InstallGuideContentPanel() {
            @Override
            protected void setupPanel() {
                add(new UrlLabel("Quickstart", "https://appland.com/docs/quickstart"));
                add(new UrlLabel("AppMap overview", "https://appland.com/docs/appmap-overview"));
                add(new UrlLabel("How to use AppMap diagrams", "https://appland.com/docs/how-to-use-appmap-diagrams"));
                add(new UrlLabel("Reference", "https://appland.com/docs/reference"));
                add(new UrlLabel("Troubleshooting", "https://appland.com/docs/troubleshooting"));
                add(new UrlLabel("Recording methods", "https://appland.com/docs/recording-methods"));
                add(new UrlLabel("Community", "https://appland.com/docs/community"));
                add(new UrlLabel("FAQ", "https://appland.com/docs/faq"));
            }
        });
    }

    private void addPanel(@NotNull String title, @NotNull InstallGuideContentPanel panel) {
        add(new CollapsibleInstallGuidePanel(panel, false, title));
    }

    @NotNull
    private static InstallGuideStatus getStatus(@NotNull InstallGuideViewPage page) {
        return InstallGuideStatus.Incomplete;
    }
}
