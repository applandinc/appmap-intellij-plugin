package appland.toolwindow.milestones;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.panels.VerticalLayout;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.LinkedList;
import java.util.List;

import static appland.toolwindow.milestones.MilestoneActions.installAppMapAgent;

/**
 * Manages a list of collapsible user milestone panels.
 */
public class UserMilestonesPanel extends JPanel {
    private final List<CollapsibleUserMilestonePanel> collapsiblePanels = new LinkedList<>();

    public UserMilestonesPanel(@NotNull Project project) {
        super(new VerticalLayout(5));

        addPanel("Quickstart", new UserMilestoneContentPanel() {
            @Override
            protected void setupPanel() {
                add(new StatusLabel(UserMilestoneStatus.Incomplete, "Install AppMap extension", () -> installAppMapAgent(project)));
                add(new StatusLabel(UserMilestoneStatus.Incomplete, "Setup AppMap agent"));
                add(new StatusLabel(UserMilestoneStatus.Incomplete, "Create AppMaps"));
                add(new StatusLabel(UserMilestoneStatus.Incomplete, "View your AppMaps"));
            }
        });
        addPanel("Using AppMaps", new UserMilestoneContentPanel() {
            @Override
            protected void setupPanel() {
                add(new StatusLabel(UserMilestoneStatus.Incomplete, "First step"));
                add(new StatusLabel(UserMilestoneStatus.Incomplete, "Second step"));
            }
        });
        addPanel("Mastermind AppsMaps", new UserMilestoneContentPanel() {
            @Override
            protected void setupPanel() {
                add(new StatusLabel(UserMilestoneStatus.Incomplete, "First step"));
                add(new StatusLabel(UserMilestoneStatus.Incomplete, "Second step"));
            }
        });
        addPanel("Support", new UserMilestoneContentPanel() {
            @Override
            protected void setupPanel() {
                add(new StatusLabel(UserMilestoneStatus.Incomplete, "First step"));
                add(new StatusLabel(UserMilestoneStatus.Incomplete, "Second step"));
            }
        });

        // demo purpose
        collapsiblePanels.get(0).setNotificationCount(1);
    }

    private void addPanel(@NotNull String title, @NotNull UserMilestoneContentPanel panel) {
        var collapsiblePanel = new CollapsibleUserMilestonePanel(panel, true, title);
        collapsiblePanels.add(collapsiblePanel);
        add(collapsiblePanel);

        // collapse all other panels when a panel was expanded
        collapsiblePanel.addCollapsingListener((collapsedPanel, isCollapsed) -> {
            if (!isCollapsed) {
                for (CollapsibleUserMilestonePanel p : collapsiblePanels) {
                    if (!collapsedPanel.equals(p)) {
                        p.setCollapsed(true);
                    }
                }
            }
        }, null);
    }
}
