package appland.toolwindow.milestones;

import com.intellij.ui.components.panels.VerticalLayout;
import com.intellij.util.ui.JBUI;

import javax.swing.*;

abstract class UserMilestoneContentPanel extends JPanel {
    UserMilestoneContentPanel() {
        super(new VerticalLayout(0));
        setBorder(JBUI.Borders.emptyLeft(15));
        setupPanel();
    }

    protected abstract void setupPanel();
}
