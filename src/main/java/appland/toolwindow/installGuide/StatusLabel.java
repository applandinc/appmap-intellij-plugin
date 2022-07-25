package appland.toolwindow.installGuide;

import com.intellij.ui.HyperlinkAdapter;
import com.intellij.ui.components.JBLabel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

/**
 * Label representing a single item of user milestones, e.g. "Install AppMap extension".
 */
class StatusLabel extends JBLabel {
    private final @Nullable Runnable clickAction;

    StatusLabel(@NotNull InstallGuideStatus status, @NotNull String label, @Nullable Runnable clickAction) {
        super(clickAction == null ? label : createLink(label), status.getIcon(), LEADING);
        this.setCopyable(true);
        this.clickAction = clickAction;
    }

    private static String createLink(@NotNull String label) {
        return String.format("<html><a href='action://'>%s</a></html>", label);
    }

    @Override
    protected @NotNull HyperlinkListener createHyperlinkListener() {
        return new HyperlinkAdapter() {
            @Override
            protected void hyperlinkActivated(HyperlinkEvent e) {
                if (clickAction != null && "action://".equals(e.getDescription())) {
                    clickAction.run();
                }
            }
        };
    }

    void setStatus(InstallGuideStatus status) {
        setIcon(status.getIcon());
    }
}
