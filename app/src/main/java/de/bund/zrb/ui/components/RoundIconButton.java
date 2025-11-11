package de.bund.zrb.ui.components;

import javax.swing.*;
import java.awt.*;

/**
 * Kleiner runder Icon-Button (blauer Kreis mit weißem Zeichen).
 */
public class RoundIconButton extends JButton {
    public RoundIconButton(String text) {
        super(text);
        setOpaque(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setForeground(Color.WHITE);
        setFont(getFont().deriveFont(Font.BOLD));
        setPreferredSize(new Dimension(22, 22));
        setMinimumSize(new Dimension(22, 22));
        setMaximumSize(new Dimension(22, 22));
        setMargin(new Insets(0,0,0,0));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setToolTipText("Hilfe anzeigen");
    }
    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            // Hintergrund (blauer Kreis)
            g2.setColor(new Color(0x1E88E5));
            g2.fillOval(0, 0, w - 1, h - 1);
            // Text mittig
            String txt = getText();
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(txt);
            int th = fm.getAscent();
            int x = (w - tw) / 2;
            int y = (h + th) / 2 - 2;
            g2.setColor(Color.WHITE);
            g2.drawString(txt, x, y);
        } finally {
            g2.dispose();
        }
    }
}

