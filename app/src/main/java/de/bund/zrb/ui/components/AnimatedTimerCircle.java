package de.bund.zrb.ui.components;

import javax.swing.*;
import java.awt.*;

/**
 * Zeichnet einen runden, animierten Countdown-Kreis mit einer eingebetteten Zeitangabe.
 */
public class AnimatedTimerCircle extends JComponent {

    private final JLabel centerLabel;
    private float progress = 1.0f; // 1.0 = voller Kreis, 0.0 = leer

    public AnimatedTimerCircle(JLabel centerLabel) {
        this.centerLabel = centerLabel;
        setLayout(new GridBagLayout());
        setOpaque(false);
        add(centerLabel);
    }

    /**
     * Setzt den Fortschritt (zwischen 0.0 und 1.0), der als Kreis angezeigt wird.
     */
    public void setProgress(float progress) {
        this.progress = progress;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        int size = Math.min(getWidth(), getHeight());
        int arcSize = size - 10;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Transparenter Hintergrund (Bubble-Effekt)
        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);

        // Hintergrundkreis
        g2.setColor(Color.DARK_GRAY);
        g2.fillOval((getWidth() - arcSize) / 2, (getHeight() - arcSize) / 2, arcSize, arcSize);

        // Fortschrittsring
        g2.setColor(Color.ORANGE);
        int angle = (int) (360 * progress);
        g2.fillArc((getWidth() - arcSize) / 2, (getHeight() - arcSize) / 2, arcSize, arcSize, 90, -angle);

        g2.dispose();
        super.paintComponent(g);
    }
}
