package de.bund.zrb.ui.status;

import de.bund.zrb.event.Severity;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/** Create small vector icons (triangle/info/cross) independent of fonts. */
public final class SeverityIconFactory {

    private SeverityIconFactory() { }

    public static Icon icon(Severity sev, int size) {
        if (sev == null) return null;
        size = Math.max(12, size);
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (sev == Severity.WARN) {
                paintWarn(g, size);
            } else if (sev == Severity.ERROR) {
                paintError(g, size);
            } else {
                paintInfo(g, size);
            }
        } finally {
            g.dispose();
        }
        return new ImageIcon(img);
    }

    // Draw yellow triangle with black exclamation
    private static void paintWarn(Graphics2D g, int s) {
        int pad = s / 8;
        Polygon tri = new Polygon();
        tri.addPoint(s/2, pad);
        tri.addPoint(s - pad, s - pad);
        tri.addPoint(pad, s - pad);
        g.setColor(new Color(255, 204, 0)); // yellow
        g.fillPolygon(tri);
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(Math.max(1f, s/24f)));
        g.drawPolygon(tri);
        // exclamation
        int barW = Math.max(1, s/8);
        int barH = s/2;
        int x = s/2 - barW/2;
        int y = s/3 - barH/3;
        g.fillRoundRect(x, y, barW, barH, barW, barW);
        int dot = Math.max(2, s/8);
        g.fillOval(s/2 - dot/2, s - pad - dot - dot/2, dot, dot);
    }

    // Draw red circle with white cross
    private static void paintError(Graphics2D g, int s) {
        int pad = s / 10;
        g.setColor(new Color(220, 0, 0));
        g.fillOval(pad, pad, s - 2*pad, s - 2*pad);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(Math.max(2f, s/10f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int a = pad*2, b = s - pad*2;
        g.drawLine(a, a, b, b);
        g.drawLine(b, a, a, b);
    }

    // Draw blue circle with white 'i'
    private static void paintInfo(Graphics2D g, int s) {
        int pad = s / 10;
        g.setColor(new Color(30, 144, 255));
        g.fillOval(pad, pad, s - 2*pad, s - 2*pad);
        g.setColor(Color.WHITE);
        int barW = Math.max(2, s/8);
        int barH = s/2;
        int x = s/2 - barW/2;
        int y = s/3;
        g.fillRoundRect(x, y, barW, barH, barW, barW);
        int dot = Math.max(2, s/8);
        g.fillOval(s/2 - dot/2, y - pad - dot, dot, dot);
    }
}
