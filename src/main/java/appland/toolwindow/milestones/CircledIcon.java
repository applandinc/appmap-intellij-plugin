package appland.toolwindow.milestones;

import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.util.Objects;

/**
 * An icon which paints a circle with text on top.
 */
public class CircledIcon implements Icon {
    private final int size;
    private final Color color;
    private final Color background;
    private final String text;
    private Font font;
    @Nullable
    private Rectangle textBounds;
    private FontRenderContext fontContext;

    public CircledIcon(int size, Color color, Color background, String text) {
        this.size = size;
        this.color = color;
        this.background = background;
        this.text = text;
    }

    private static Rectangle getPixelBounds(Font font, String text, FontRenderContext context) {
        return font.hasLayoutAttributes()
                ? new TextLayout(text, font, context).getPixelBounds(context, 0, 0)
                : font.createGlyphVector(context, text).getPixelBounds(context, 0, 0);
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        g.setColor(background);
        g.fillOval(x, y, size, size);

        var bounds = getTextBounds();
        if (bounds != null) {
            var g2d = (Graphics2D) g.create();
            try {
                g2d.setColor(color);
                g2d.setFont(font);

                Object textLcdContrast = UIManager.get(RenderingHints.KEY_TEXT_LCD_CONTRAST);
                if (textLcdContrast == null) {
                    textLcdContrast = UIUtil.getLcdContrastValue(); // L&F is not properly updated
                }
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, fontContext.getAntiAliasingHint());
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_LCD_CONTRAST, textLcdContrast);
                g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, fontContext.getFractionalMetricsHint());
                g2d.drawString(text, (float) (size - bounds.width) / 2.0f + 1.0f, (float) (size + bounds.height) / 2.0f + 0.0f);
            } finally {
                g2d.dispose();
            }

        }
    }

    @Override
    public int getIconWidth() {
        return size;
    }

    @Override
    public int getIconHeight() {
        return size;
    }

    @Override
    public int hashCode() {
        return Objects.hash(size, color);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CircledIcon that = (CircledIcon) o;
        return size == that.size && Objects.equals(color, that.color);
    }

    public void setFont(Font font) {
        this.font = font;
    }

    // inspired by com.intellij.ui.TextIcon
    private Rectangle getTextBounds() {
        if (textBounds == null && font != null && text != null && !text.isEmpty()) {
            Object aaHint = UIManager.get(RenderingHints.KEY_TEXT_ANTIALIASING);
            if (aaHint == null) aaHint = RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT;
            Object fmHint = UIManager.get(RenderingHints.KEY_FRACTIONALMETRICS);
            if (fmHint == null) fmHint = RenderingHints.VALUE_FRACTIONALMETRICS_DEFAULT;
            fontContext = new FontRenderContext(null, aaHint, fmHint);
            textBounds = getPixelBounds(font, text, fontContext);
        }
        return textBounds;
    }
}
