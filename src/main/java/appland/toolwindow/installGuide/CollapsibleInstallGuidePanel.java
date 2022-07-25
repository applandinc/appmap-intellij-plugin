package appland.toolwindow.installGuide;

import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;

/**
 * A collapsible panel with a title (always visible) and content (visible only if expanded).
 */
class CollapsibleInstallGuidePanel extends JPanel {
    private final Collection<CollapsingListener> listeners = ContainerUtil.createLockFreeCopyOnWriteList();
    private final JComponent content;
    private final InstallGuideTitlePanel title;
    private boolean isCollapsed;
    private boolean isInitialized = false;

    public CollapsibleInstallGuidePanel(JComponent content, boolean isCollapsed, @NotNull String title) {
        super(new BorderLayout());

        this.content = content;

        this.title = new InstallGuideTitlePanel(title, isCollapsed);
        this.title.addLabelActionListener(() -> setCollapsed(!isCollapsed()));
        add(this.title, BorderLayout.NORTH);

        setFocusable(false);
        setCollapsed(isCollapsed);
    }

    public boolean isCollapsed() {
        return isCollapsed;
    }

    protected void setCollapsed(boolean collapse) {
        try {
            if (!collapse) {
                add(content, BorderLayout.CENTER);
            } else if (isInitialized) {
                remove(content);
            }
            isCollapsed = collapse;
            title.setCollapsed(isCollapsed);

            notifyListeners();

            revalidate();
            repaint();
        } finally {
            isInitialized = true;
        }
    }

    private void notifyListeners() {
        for (CollapsingListener listener : listeners) {
            listener.onCollapsingChanged(this, isCollapsed());
        }
    }

    interface CollapsingListener {
        void onCollapsingChanged(@NotNull CollapsibleInstallGuidePanel panel, boolean isCollapsed);
    }
}