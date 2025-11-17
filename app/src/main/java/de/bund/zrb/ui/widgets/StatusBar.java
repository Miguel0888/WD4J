package de.bund.zrb.ui.widgets;

import de.bund.zrb.event.ApplicationEventBus;
import de.bund.zrb.event.SavedEntityEvent;
import de.bund.zrb.service.SettingsService;

import javax.swing.*;
import java.awt.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;

public final class StatusBar extends JPanel {
    private final JLabel leftLabel = new JLabel("Bereit");
    private final JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
    private final JButton actionButton = new JButton();

    // Verlauf der Statusmeldungen, begrenzt über Setting "statusbar.history.limit"
    private final Deque<String> history = new ArrayDeque<>();

    public StatusBar(JComponent rightComponent) {
        super(new BorderLayout());

        // Farben ggf. aus Settings übernehmen (Design-Anpassung)
        Color bg = getColor("statusbar.color.bg");
        Color fg = getColor("statusbar.color.fg");
        if (bg == null) {
            bg = UIManager.getColor("Panel.background");
        }
        if (fg == null) {
            fg = UIManager.getColor("Label.foreground");
        }
        setBackground(bg);
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0,0,0,50)));

        centerPanel.setOpaque(false);
        leftLabel.setOpaque(false);
        leftLabel.setForeground(fg);
        centerPanel.add(leftLabel);
        actionButton.setVisible(false);
        actionButton.setFocusable(false);
        actionButton.setMargin(new Insets(2,8,2,8));
        actionButton.setForeground(fg);
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
            String txt = "Gespeichert: " + p.entityType + "  " + (p.name != null ? p.name : "(unnamed)")
                    + (time.isEmpty() ? "" : " (" + time + ")");
            setMessage(txt);
        });
    }

    /** Set only text (keeps old icon). Thread-safe. */
    public void setMessage(String text) { setMessage(text, null, false); hideAction(); }

    /** Set text and (optionally) replace icon. Thread-safe. */
    public void setMessage(final String text, final Icon icon, final boolean replaceIcon) {
        if (SwingUtilities.isEventDispatchThread()) {
            applyMessage(text, icon, replaceIcon);
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    applyMessage(text, icon, replaceIcon);
                }
            });
        }
    }

    private void applyMessage(String text, Icon icon, boolean replaceIcon) {
        String t = text != null ? text : "";
        leftLabel.setText(t);
        if (replaceIcon) leftLabel.setIcon(icon);

        // Verlauf pflegen: nur begrenzen, keine extra Flags
        Integer limit = SettingsService.getInstance().get("statusbar.history.limit", Integer.class);
        int max = (limit != null && limit > 0) ? limit : 100;
        if (t.length() > 0) {
            history.addLast(t);
            while (history.size() > max) {
                history.removeFirst();
            }
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
        applyMessage(text, null, false);
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

    // Hilfsmethode für Farbschlüssel
    private static Color getColor(String key) {
        Integer rgb = SettingsService.getInstance().get(key, Integer.class);
        return rgb != null ? new Color(rgb, true) : null;
    }

    // Debug/Tests: aktuelle Anzahl der Verlaufseinträge
    int getHistorySize() { return history.size(); }
}
