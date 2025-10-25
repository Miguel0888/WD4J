package de.bund.zrb.ui.debug;

import de.bund.zrb.event.ApplicationEventBus;
import de.bund.zrb.event.EventServiceControlRequestedEvent;
import de.bund.zrb.websocket.WDEventNames;

import javax.swing.*;
import java.awt.*;
import java.util.EnumMap;

public final class EventMonitorWindow extends JFrame {
    private final JPanel header = new JPanel(new BorderLayout(6, 6));
    private final JPanel controlRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
    private final JPanel checkboxPanel = new JPanel(new WrapFlowLayout(FlowLayout.LEFT, 8, 4)); // Wrap!
    private final JPanel list = new JPanel();

    private final JButton btnStart = new JButton("Start");
    private final JButton btnStop  = new JButton("Stop");

    private final JTextArea detailArea = new JTextArea();
    private final JScrollPane detailScroll = new JScrollPane(detailArea);
    private JComponent selectedRow;

    // Flags kommen aus der Session; wir halten nur den letzten Stand zum Filtern
    private EnumMap<WDEventNames, Boolean> flags = new EnumMap<>(WDEventNames.class);

    public EventMonitorWindow(String username) {
        super("Events – " + username);
        setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        setSize(960, 560);
        setLocationByPlatform(true);

        // Linke Liste
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        JScrollPane scroll = new JScrollPane(list);

        // Clear
        JButton btnClear = new JButton("Clear");
        btnClear.setFocusable(false);
        btnClear.addActionListener(e -> {
            list.removeAll();
            list.revalidate();
            list.repaint();
            detailArea.setText("");
            selectedRow = null;
        });

        // Start/Stop -> EventBus
        btnStart.setFocusable(false);
        btnStart.addActionListener(e ->
                ApplicationEventBus.getInstance().publish(
                        new EventServiceControlRequestedEvent(EventServiceControlRequestedEvent.Operation.START)
                ));
        btnStop.setFocusable(false);
        btnStop.addActionListener(e ->
                ApplicationEventBus.getInstance().publish(
                        new EventServiceControlRequestedEvent(EventServiceControlRequestedEvent.Operation.STOP)
                ));

        // obere Button-Reihe
        controlRow.add(new JLabel("Events"));
        controlRow.add(btnClear);
        controlRow.add(Box.createHorizontalStrut(8));
        controlRow.add(btnStart);
        controlRow.add(btnStop);

        // Checkbox-Zeile darunter (wrappt bei wenig Platz), in Scroll zur Sicherheit
        JScrollPane checkboxScroll = new JScrollPane(checkboxPanel);
        checkboxScroll.setBorder(BorderFactory.createEmptyBorder());
        checkboxScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        checkboxScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        checkboxScroll.getVerticalScrollBar().setUnitIncrement(12);

        // Header: oben Controls, darunter Checkboxen
        header.add(controlRow, BorderLayout.NORTH);
        header.add(checkboxScroll, BorderLayout.CENTER);

        // Details rechts
        detailArea.setEditable(false);
        detailArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        detailArea.setLineWrap(false);
        detailScroll.setBorder(BorderFactory.createTitledBorder("Details"));
        detailScroll.setMinimumSize(new Dimension(320, 0)); // immer sichtbar
        detailScroll.setPreferredSize(new Dimension(380, 0));

        JPanel left = new JPanel(new BorderLayout(6, 6));
        left.add(header, BorderLayout.NORTH);
        left.add(scroll, BorderLayout.CENTER);
        left.setMinimumSize(new Dimension(320, 0));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, detailScroll);
        split.setContinuousLayout(true);
        split.setResizeWeight(0.58);

        // Divider nach dem Realisieren setzen, damit rechts nicht verschwindet
        SwingUtilities.invokeLater(() -> split.setDividerLocation(0.58));

        JPanel content = new JPanel(new BorderLayout(6, 6));
        content.add(split, BorderLayout.CENTER);
        setContentPane(content);
    }

    // Flags abrufen
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
        if (name instanceof String) visible = isEnabled((String) name);
        c.setVisible(visible);

        // Selektieren + Details anzeigen
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

    /** FlowLayout, das Höhe so berechnet, dass bei schmalem Fenster auf mehrere Zeilen umbrochen wird. */
    static final class WrapFlowLayout extends FlowLayout {
        public WrapFlowLayout(int align, int hgap, int vgap) { super(align, hgap, vgap); }
        @Override public Dimension preferredLayoutSize(Container target) { return layoutSize(target, true); }
        @Override public Dimension minimumLayoutSize(Container target) { return layoutSize(target, false); }
        private Dimension layoutSize(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                int maxWidth = target.getWidth();
                if (maxWidth <= 0) maxWidth = Integer.MAX_VALUE;

                int hgap = getHgap(), vgap = getVgap();
                Insets insets = target.getInsets();
                int max = maxWidth - (insets.left + insets.right + hgap * 2);

                int x = 0, y = vgap, rowHeight = 0;
                for (Component m : target.getComponents()) {
                    if (!m.isVisible()) continue;
                    Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
                    if (x == 0 || x + d.width <= max) {
                        if (x > 0) x += hgap;
                        x += d.width;
                        rowHeight = Math.max(rowHeight, d.height);
                    } else {
                        x = d.width;
                        y += vgap + rowHeight;
                        rowHeight = d.height;
                    }
                }
                y += rowHeight + vgap;
                return new Dimension(maxWidth, y + insets.top + insets.bottom);
            }
        }
    }
}
