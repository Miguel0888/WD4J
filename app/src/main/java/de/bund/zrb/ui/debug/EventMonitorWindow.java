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

    private final JTextArea detailArea = new JTextArea();
    private final JScrollPane detailScroll = new JScrollPane(detailArea);
    private JComponent selectedRow;

    // Start/Stop wieder hinzufügen
    private final JButton btnStart = new JButton("Start");
    private final JButton btnStop  = new JButton("Stop");

    // Flags kommen aus der Session; wir halten nur den letzten Stand zum Filtern
    private EnumMap<WDEventNames, Boolean> flags = new EnumMap<>(WDEventNames.class);

    public EventMonitorWindow(String username) {
        super("Events – " + username);
        setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        setSize(960, 560); // etwas breiter für den Split
        setLocationByPlatform(true);

        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        JScrollPane scroll = new JScrollPane(list);

        JButton btnClear = new JButton("Clear");
        btnClear.setFocusable(false);
        btnClear.addActionListener(e -> {
            list.removeAll();
            list.revalidate();
            list.repaint();
            detailArea.setText(""); // Details leeren
            selectedRow = null;
        });

        // Start/Stop klicks: an den EventBus publizieren (wie die früheren Commands)
        btnStart.setFocusable(false);
        btnStart.addActionListener(e ->
                ApplicationEventBus.getInstance().publish(
                        new EventServiceControlRequestedEvent(EventServiceControlRequestedEvent.Operation.START)
                )
        );

        btnStop.setFocusable(false);
        btnStop.addActionListener(e ->
                ApplicationEventBus.getInstance().publish(
                        new EventServiceControlRequestedEvent(EventServiceControlRequestedEvent.Operation.STOP)
                )
        );

        header.add(new JLabel("Events"));
        header.add(btnClear);
        header.add(Box.createHorizontalStrut(8));
        header.add(btnStart);
        header.add(btnStop);
        header.add(Box.createHorizontalStrut(12));
        header.add(checkboxPanel);

        // Details rechts vorbereiten
        detailArea.setEditable(false);
        detailArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        detailArea.setLineWrap(false);
        detailScroll.setBorder(BorderFactory.createTitledBorder("Details"));

        JPanel left = new JPanel(new BorderLayout(6, 6));
        left.add(header, BorderLayout.NORTH);
        left.add(scroll, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, detailScroll);
        split.setResizeWeight(0.55);   // mehr Platz links
        split.setDividerLocation(0.55);

        JPanel content = new JPanel(new BorderLayout(6, 6));
        content.add(split, BorderLayout.CENTER);
        setContentPane(content);
    }

    //  Flags abrufen (für externes Start-Wiring nützlich)
    public EnumMap<WDEventNames, Boolean> getFlags() {
        return new EnumMap<>(this.flags);
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

        // Klick selektiert die Zeile und zeigt Details rechts
        c.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                setSelectedRow(c);
                showDetailsFrom(c);
            }
        });

        list.add(c);
        list.revalidate();
        list.repaint();
        try { list.scrollRectToVisible(c.getBounds()); } catch (Throwable ignore) {}
    }

    // visuelle Selektion und Details zeigen
    private void setSelectedRow(JComponent row) {
        if (selectedRow != null) {
            selectedRow.setBackground(null);
            selectedRow.setOpaque(false);
        }
        selectedRow = row;
        if (selectedRow != null) {
            selectedRow.setOpaque(true);
            selectedRow.setBackground(new Color(0xE6F2FF)); // dezentes Blau
            selectedRow.repaint();
        }
    }

    private void showDetailsFrom(JComponent c) {
        Object pretty = c.getClientProperty("payloadPretty");
        if (pretty instanceof String) {
            detailArea.setText((String) pretty);
            detailArea.setCaretPosition(0);
        } else {
            // Fallback: rudimentär aus eventName
            Object ev = c.getClientProperty("eventName");
            detailArea.setText(ev == null ? "" : String.valueOf(ev));
            detailArea.setCaretPosition(0);
        }
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
