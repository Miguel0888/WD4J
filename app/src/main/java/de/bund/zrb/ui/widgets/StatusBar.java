package de.bund.zrb.ui.widgets;

import javax.swing.*;
import java.awt.*;

public final class StatusBar extends JPanel {
    private final JLabel left = new JLabel("Bereit");

    public StatusBar(JComponent rightComponent) {
        super(new BorderLayout(8, 0));
        setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        left.setForeground(new Color(0, 0, 0, 150));
        add(left, BorderLayout.WEST);
        if (rightComponent != null) add(rightComponent, BorderLayout.EAST);
    }

    public void setMessage(String text) {
        left.setText(text != null ? text : "");
    }
}
