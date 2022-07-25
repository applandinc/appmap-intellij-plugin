package appland.toolwindow.installGuide;

import appland.Icons;
import com.intellij.ide.BrowserUtil;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.ui.HyperlinkAdapter;
import com.intellij.ui.components.JBLabel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

class UrlLabel extends JBLabel {
    UrlLabel(@NotNull String label, @NotNull String url) {
        super(createLink(label, url));

        // to enable link support
        setCopyable(true);

        setIcon(iconFor(url));
    }

    private static Icon iconFor(String url) {
        return isLinkVisited(url) ? Icons.LINK_VISITED : Icons.LINK;
    }

    private static String createLink(String label, String url) {
        return String.format("<html><a href='%s'>%s</a></html>", url, label);
    }

    private static boolean isLinkVisited(String url) {
        return PropertiesComponent.getInstance().isTrueValue(urlPropertyKey(url));
    }

    @NotNull
    private static String urlPropertyKey(String url) {
        return "appland.url." + url;
    }

    @Override
    protected @NotNull HyperlinkListener createHyperlinkListener() {
        return new HyperlinkAdapter() {
            @Override
            protected void hyperlinkActivated(HyperlinkEvent e) {
                var url = e.getDescription();
                BrowserUtil.browse(url);

                PropertiesComponent.getInstance().setValue(urlPropertyKey(url), true);
                setIcon(iconFor(url));
            }
        };
    }
}
