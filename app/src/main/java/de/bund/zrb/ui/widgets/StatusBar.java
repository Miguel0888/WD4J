package de.bund.zrb.ui.widgets;

import javax.swing.*;
import java.awt.*;

public final class StatusBar extends JPanel {
    private final JLabel leftLabel = new JLabel("Bereit");

    public StatusBar(JComponent rightComponent) {
        super(new BorderLayout());
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0,0,0,50)));
        add(leftLabel, BorderLayout.WEST);

        JPanel rightWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        rightWrap.setOpaque(false);
        if (rightComponent != null) rightWrap.add(rightComponent);
        add(rightWrap, BorderLayout.EAST);
    }

    /** Set only text (keeps old icon). Thread-safe. */
    public void setMessage(String text) {
        setMessage(text, null, false);
    }

    /** Set text and (optionally) replace icon. Thread-safe. */
    public void setMessage(final String text, final Icon icon, final boolean replaceIcon) {
        if (SwingUtilities.isEventDispatchThread()) {
            leftLabel.setText(text != null ? text : "");
            if (replaceIcon) leftLabel.setIcon(icon);
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    leftLabel.setText(text != null ? text : "");
                    if (replaceIcon) leftLabel.setIcon(icon);
                }
            });
        }
    }
}
