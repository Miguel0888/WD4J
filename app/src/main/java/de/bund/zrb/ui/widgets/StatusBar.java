// File: app/src/main/java/de/bund/zrb/ui/widgets/StatusBar.java
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

    /** Text links stumpf setzen. Thread-safe auf den EDT. */
    public void setMessage(String text) {
        if (SwingUtilities.isEventDispatchThread()) {
            leftLabel.setText(text != null ? text : "");
        } else {
            SwingUtilities.invokeLater(() -> leftLabel.setText(text != null ? text : ""));
        }
    }
}
