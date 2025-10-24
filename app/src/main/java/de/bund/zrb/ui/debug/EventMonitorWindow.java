package de.bund.zrb.ui.debug;

import de.bund.zrb.event.ApplicationEventBus;
import de.bund.zrb.event.EventServiceControlRequestedEvent;
import de.bund.zrb.websocket.WDEventNames;

import javax.swing.*;
import java.awt.*;
import java.util.EnumMap;

public final class EventMonitorWindow extends JFrame {
    private final JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
    private final JPanel checkboxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    private final JPanel list = new JPanel();

    // Flags kommen aus der Session; wir halten nur den letzten Stand zum Filtern
    private EnumMap<WDEventNames, Boolean> flags = new EnumMap<>(WDEventNames.class);

    public EventMonitorWindow(String username) {
        super("Events – " + username);
        setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        setSize(720, 520);
        setLocationByPlatform(true);

        // Layout
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        JScrollPane scroll = new JScrollPane(list);

        JButton btnClear = new JButton("Clear");
        btnClear.setFocusable(false);
        btnClear.addActionListener(e -> {
            list.removeAll();
            list.revalidate();
            list.repaint();
        });

        // Start/Stop-Buttons für Event-Logging (integriert neben "Clear")
        JButton btnStart = new JButton("Start");
        btnStart.setFocusable(false);
        btnStart.setToolTipText("Event-Logging starten");
        btnStart.addActionListener(e ->
                ApplicationEventBus.getInstance().publish(
                        new EventServiceControlRequestedEvent(EventServiceControlRequestedEvent.Operation.START)
                )
        );

        JButton btnStop = new JButton("Stop");
        btnStop.setFocusable(false);
        btnStop.setToolTipText("Event-Logging stoppen");
        btnStop.addActionListener(e ->
                ApplicationEventBus.getInstance().publish(
                        new EventServiceControlRequestedEvent(EventServiceControlRequestedEvent.Operation.STOP)
                )
        );

        header.add(new JLabel("Events"));
        header.add(btnClear);
        header.add(btnStart);
        header.add(btnStop);
        header.add(Box.createHorizontalStrut(12));
        header.add(checkboxPanel);

        JPanel content = new JPanel(new BorderLayout(6, 6));
        content.add(header, BorderLayout.NORTH);
        content.add(scroll, BorderLayout.CENTER);
        setContentPane(content);
    }

    public void setFlags(EnumMap<WDEventNames, Boolean> flags, Runnable onFlagChanged) {
        if (flags == null) return;
        this.flags = new EnumMap<>(flags);
        rebuildCheckboxes(onFlagChanged);
        applyFilter();
    }

    private void rebuildCheckboxes(Runnable onFlagChanged) {
        checkboxPanel.removeAll();
        for (WDEventNames ev : WDEventNames.values()) {
            if (!flags.containsKey(ev)) continue;
            JCheckBox cb = new JCheckBox(pretty(ev), Boolean.TRUE.equals(flags.get(ev)));
            cb.setFocusable(false);
            cb.setToolTipText(ev.getDescription() == null || ev.getDescription().isEmpty() ? ev.getName() : ev.getDescription());
            cb.addActionListener(ae -> {
                flags.put(ev, cb.isSelected());
                applyFilter();
                if (onFlagChanged != null) onFlagChanged.run();
            });
            checkboxPanel.add(cb);
        }
        checkboxPanel.revalidate();
        checkboxPanel.repaint();
    }

    public void appendMeta(String line) {
        if (line == null) return;
        JLabel lbl = new JLabel(line);
        lbl.putClientProperty("eventName", null);
        append(lbl);
    }

    public void appendEvent(String bidiEventName, JComponent component) {
        if (component == null) return;
        component.putClientProperty("eventName", bidiEventName);
        append(component);
    }

    private void append(JComponent c) {
        boolean visible = true;
        Object name = c.getClientProperty("eventName");
        if (name instanceof String) {
            visible = isEnabled((String) name);
        }
        c.setVisible(visible);
        list.add(c);
        list.revalidate();
        list.repaint();
        try { list.scrollRectToVisible(c.getBounds()); } catch (Throwable ignore) {}
    }

    private void applyFilter() {
        for (Component c : list.getComponents()) {
            boolean visible = true;
            if (c instanceof JComponent) {
                Object name = ((JComponent) c).getClientProperty("eventName");
                if (name instanceof String) visible = isEnabled((String) name);
            }
            c.setVisible(visible);
        }
        list.revalidate(); list.repaint();
    }

    private boolean isEnabled(String bidiEventName) {
        if (bidiEventName == null) return true;
        for (WDEventNames ev : WDEventNames.values()) {
            if (ev.getName().equals(bidiEventName)) {
                Boolean on = flags.get(ev);
                return on == null || on.booleanValue();
            }
        }
        return true;
    }

    private static String pretty(WDEventNames ev) {
        switch (ev) {
            case BEFORE_REQUEST_SENT: return "request";
            case RESPONSE_STARTED:    return "response";
            case RESPONSE_COMPLETED:  return "done";
            case FETCH_ERROR:         return "error";
            case DOM_CONTENT_LOADED:  return "dom";
            case LOAD:                return "load";
            case ENTRY_ADDED:         return "console";
            case CONTEXT_CREATED:     return "ctx+";
            case CONTEXT_DESTROYED:   return "ctx-";
            case FRAGMENT_NAVIGATED:  return "hash";
            case NAVIGATION_STARTED:  return "nav";
            default: return ev.name().toLowerCase().replace('_', ' ');
        }
    }
}
