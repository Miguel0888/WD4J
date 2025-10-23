// File: app/src/main/java/de/bund/zrb/ui/status/StatusBarManager.java
package de.bund.zrb.ui.status;

import javax.swing.*;
import java.awt.*;

public final class StatusBarManager {
    private static final StatusBarManager INSTANCE = new StatusBarManager();
    public static StatusBarManager getInstance() { return INSTANCE; }

    private final JLabel leftLabel  = new JLabel("Bereit");
    private final JPanel rightBox   = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 2));
    private final JPanel root       = new JPanel(new BorderLayout());

    private StatusBarManager() {
        root.setBorder(BorderFactory.createMatteBorder(1,0,0,0, new Color(0,0,0,50)));
        root.add(leftLabel, BorderLayout.WEST);
        root.add(rightBox,   BorderLayout.EAST);
    }

    /** Liefert die fertige Statusbar-Komponente zur Platzierung im SOUTH. */
    public JComponent getComponent() { return root; }

    /** Setzt die Nachricht links. Thread-sicher für die EDT. */
    public void setMessage(String msg) {
        runOnEdt(() -> leftLabel.setText(msg != null ? msg : ""));
    }

    /** Ersetzt alles rechts durch die gegebene Komponente (z.B. Label oder Button). */
    public void setRightComponent(JComponent comp) {
        runOnEdt(() -> {
            rightBox.removeAll();
            if (comp != null) rightBox.add(comp);
            rightBox.revalidate();
            rightBox.repaint();
        });
    }

    /** Zeigt rechts einfachen Text (falls du keine eigene Komponente brauchst). */
    public void setRightText(String text) {
        JLabel lbl = new JLabel(text != null ? text : "");
        setRightComponent(lbl);
    }

    private static void runOnEdt(Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) r.run();
        else SwingUtilities.invokeLater(r);
    }
}
