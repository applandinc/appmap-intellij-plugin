package appland.toolwindow.milestones;

import com.intellij.ide.ui.laf.darcula.DarculaUIUtil;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.StartupUiUtil;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.LinkedList;
import java.util.List;

class UserMilestoneTitlePanel extends JPanel {
    @NonNls public static final String ENTER = "enter";
    @NonNls public static final String SPACE = "space";
    private static final KeyStroke KEY_ENTER = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
    private static final KeyStroke KEY_SPACE = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0);
    private final JBLabel label;
    private final JBLabel notification = new JBLabel();
    private final Icon expandIcon = UIUtil.getTreeCollapsedIcon();
    private final Icon collapseIcon = UIUtil.getTreeExpandedIcon();

    private final List<Runnable> labelActionListeners = new LinkedList<>();

    UserMilestoneTitlePanel(@NotNull String title, boolean isCollapsed) {
        super(new BorderLayout());

        label = new JBLabel(title, SwingConstants.LEADING);
        label.setIconTextGap(JBUI.scale(5));
        label.setFont(StartupUiUtil.getLabelFont().deriveFont(Font.BOLD));
        label.setIcon(isCollapsed ? collapseIcon : expandIcon);
        add(label, BorderLayout.CENTER);

        setFocusable(true);
        setBorder(JBUI.Borders.empty(3));

        getActionMap().put(ENTER, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                executeLabelAction();
            }
        });
        getActionMap().put(SPACE, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                executeLabelAction();
            }
        });
        getInputMap().put(KEY_ENTER, ENTER);
        getInputMap().put(KEY_SPACE, SPACE);

        addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                repaint();
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    executeLabelAction();
                }
            }
        });
    }

    @NotNull
    private static Icon createNotificationIcon(int count) {
        var icon = new CircledIcon(24, Color.WHITE, ColorUtil.fromHex("0E639C"), count < 10 ? String.valueOf(count) : "9+");
        icon.setFont(UIUtil.getLabelFont(UIUtil.FontSize.MINI).deriveFont(Font.BOLD));
        return icon;
    }

    public void setCollapsed(boolean isCollapsed) {
        this.label.setIcon(isCollapsed ? collapseIcon : expandIcon);
    }

    public void setNotificationCount(int count) {
        if (count >= 0) {
            notification.setIcon(createNotificationIcon(count));
            add(notification, BorderLayout.EAST);
            revalidate();
            repaint();
        } else {
            remove(notification);
        }
    }

    private void executeLabelAction() {
        for (var runnable : labelActionListeners) {
            runnable.run();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (isFocusOwner() && g instanceof Graphics2D) {
            DarculaUIUtil.paintFocusBorder((Graphics2D) g, getWidth(), getHeight(), 0f, true);
        }
    }

    void addLabelActionListener(@NotNull Runnable listener) {
        this.labelActionListeners.add(listener);
    }
}
