package de.bund.zrb.ui.widgets;

import de.bund.zrb.event.ApplicationEventBus;
import de.bund.zrb.event.SavedEntityEvent;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import javax.swing.*;
import java.awt.*;

public final class StatusBar extends JPanel {
    private final JLabel leftLabel = new JLabel("Bereit");
    private final JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
    private final JButton actionButton = new JButton();

    public StatusBar(JComponent rightComponent) {
        super(new BorderLayout());
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0,0,0,50)));

        centerPanel.setOpaque(false);
        leftLabel.setOpaque(false);
        centerPanel.add(leftLabel);
        actionButton.setVisible(false);
        actionButton.setFocusable(false);
        actionButton.setMargin(new Insets(2,8,2,8));
        centerPanel.add(actionButton);
        add(centerPanel, BorderLayout.WEST);

        JPanel rightWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        rightWrap.setOpaque(false);
        if (rightComponent != null) rightWrap.add(rightComponent);
        add(rightWrap, BorderLayout.EAST);

        ApplicationEventBus.getInstance().subscribe(SavedEntityEvent.class, ev -> {
            SavedEntityEvent.Payload p = ev.getPayload();
            String time = p.timestamp == null ? "" : DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
                    .format(p.timestamp.atZone(ZoneId.systemDefault()));
            String txt = "Gespeichert: " + p.entityType + " – " + (p.name != null ? p.name : "(unnamed)")
                    + (time.isEmpty() ? "" : " (" + time + ")");
            setMessage(txt);
        });
    }

    /** Set only text (keeps old icon). Thread-safe. */
    public void setMessage(String text) { setMessage(text, null, false); hideAction(); }

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

    public void setMessageWithAction(String text, String buttonLabel, Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) {
            applyMessageWithAction(text, buttonLabel, action);
        } else {
            SwingUtilities.invokeLater(() -> applyMessageWithAction(text, buttonLabel, action));
        }
    }

    private void applyMessageWithAction(String text, String buttonLabel, Runnable action) {
        leftLabel.setText(text != null ? text : "");
        if (buttonLabel != null && action != null) {
            actionButton.setText(buttonLabel);
            for (java.awt.event.ActionListener l : actionButton.getActionListeners()) actionButton.removeActionListener(l);
            actionButton.addActionListener(e -> action.run());
            actionButton.setVisible(true);
        } else {
            hideAction();
        }
        revalidate(); repaint();
    }

    private void hideAction() {
        actionButton.setVisible(false);
        for (java.awt.event.ActionListener l : actionButton.getActionListeners()) actionButton.removeActionListener(l);
    }
}
