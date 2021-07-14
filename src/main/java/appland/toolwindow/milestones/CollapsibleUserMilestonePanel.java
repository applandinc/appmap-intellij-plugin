package appland.toolwindow.milestones;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;

/**
 * A collapsible panel with a title (always visible) and content (visible only if expanded).
 */
class CollapsibleUserMilestonePanel extends JPanel {
    private final Collection<CollapsingListener> listeners = ContainerUtil.createLockFreeCopyOnWriteList();
    private final JComponent content;
    private final UserMilestoneTitlePanel title;
    private boolean isCollapsed;
    private boolean isInitialized = false;

    public CollapsibleUserMilestonePanel(JComponent content, boolean isCollapsed, @NotNull String title) {
        super(new BorderLayout());

        this.content = content;

        this.title = new UserMilestoneTitlePanel(title, isCollapsed);
        this.title.addLabelActionListener(() -> setCollapsed(!isCollapsed()));
        add(this.title, BorderLayout.NORTH);

        setFocusable(false);
        setCollapsed(isCollapsed);
    }

    public final void setNotificationCount(int count) {
        this.title.setNotificationCount(count);
    }

    public void addCollapsingListener(@NotNull CollapsingListener listener, @Nullable Disposable parent) {
        listeners.add(listener);

        if (parent != null) {
            Disposer.register(parent, () -> listeners.remove(listener));
        }
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
        void onCollapsingChanged(@NotNull CollapsibleUserMilestonePanel panel, boolean isCollapsed);
    }
}