package de.bund.zrb.ui.components;

import de.bund.zrb.PageImpl;
import de.bund.zrb.dto.GrowlNotification;
import de.bund.zrb.service.BrowserServiceImpl;
import de.bund.zrb.service.NotificationService;
import de.bund.zrb.service.ToolsRegistry;
import de.bund.zrb.service.RegexPatternRegistry;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * Visualize growl notifications, await patterns, and manage history.
 * - Title- and Message-Regex on separate rows, each as editable presets (combo).
 * - Timeout in seconds (double) with 10.0s step; converts to ms internally.
 * - "Await + groups" returns and displays all capture groups from the message-regex.
 * - Clear history directly from the dialog.
 * - Load regex presets from RegexPatternRegistry (regex.json).
 */
public class NotificationTestDialog extends JDialog {

    private final JComboBox<String> cbSeverity;
    private final JComboBox<String> cbTitleRegex;
    private final JComboBox<String> cbMessageRegex;
    private final JSpinner spTimeoutSeconds;
    private final JButton btnAwait;
    private final JButton btnAwaitAndGroups;
    private final JButton btnClear;

    private final JTable table;
    private final DefaultTableModel model;
    private final JLabel lbStatus;

    private NotificationService service;
    private Consumer<List<GrowlNotification>> listenerRef;

    private static final String[] COLS = {"Zeit", "Type", "Title", "Message", "Context"};
    private static final SimpleDateFormat TS_FMT = new SimpleDateFormat("HH:mm:ss.SSS");

    public NotificationTestDialog(Window parent) {
        super(parent, "Growl/Notification-Tester", ModalityType.MODELESS);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(1180, 600));
        setLocationRelativeTo(parent);

        // --- Controls ---
        cbSeverity = new JComboBox<String>(new String[]{"ANY","INFO","WARN","ERROR","FATAL"});

        // Load presets from registry (falls leer, liefert Registry Defaults)
        RegexPatternRegistry reg = RegexPatternRegistry.getInstance();
        cbTitleRegex   = createEditableRegexCombo(reg.getTitlePresets(), 48);
        cbMessageRegex = createEditableRegexCombo(reg.getMessagePresets(), 64);

        spTimeoutSeconds = new JSpinner(new SpinnerNumberModel(30.0d, 0.1d, 3600.0d, 10.0d)); // step 10s
        ((JSpinner.DefaultEditor) spTimeoutSeconds.getEditor()).getTextField().setColumns(6);

        btnAwait = new JButton("Await");
        btnAwaitAndGroups = new JButton("Await + groups");
        btnClear = new JButton("Clear history");

        // --- Layout root ---
        final JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Top (filters + actions)
        root.add(buildNorthPanel(), BorderLayout.NORTH);

        // Center (table)
        model = new DefaultTableModel(COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        configureTable(table);
        root.add(new JScrollPane(table), BorderLayout.CENTER);

        // Status bar
        lbStatus = new JLabel("Bereit.");
        lbStatus.setBorder(new EmptyBorder(8, 2, 2, 2));
        root.add(lbStatus, BorderLayout.SOUTH);

        setContentPane(root);

        // Wire actions
        btnAwait.addActionListener(e -> doAwait(false));
        btnAwaitAndGroups.addActionListener(e -> doAwait(true));
        btnClear.addActionListener(e -> {
            if (service != null) {
                service.clearHistory();
                lbStatus.setText("History gelöscht.");
            }
        });

        // ESC → close
        installEscToClose();

        // Resolve & subscribe
        resolveServiceForActivePage();
        hookLiveUpdates();
    }

    // ---------- UI building ----------

    private JPanel buildNorthPanel() {
        JPanel north = new JPanel(new BorderLayout(8, 8));

        // Filters panel (left)
        JPanel filters = new JPanel(new GridBagLayout());
        filters.setBorder(new TitledBorder("Filter"));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 6, 4, 6);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;

        int row = 0;
        int col;

        // Row 0: Severity + Timeout(s)
        col = 0;
        gc.gridy = row; gc.gridx = col++; gc.weightx = 0;
        filters.add(new JLabel("Severity:"), gc);
        gc.gridx = col++; gc.weightx = 0.2;
        filters.add(cbSeverity, gc);

        gc.gridx = col++; gc.weightx = 0;
        filters.add(new JLabel("Timeout (s):"), gc);
        gc.gridx = col++; gc.weightx = 0.2;
        filters.add(spTimeoutSeconds, gc);

        // Row 1: Title-Regex (own line)
        row++; col = 0;
        gc.gridy = row; gc.gridx = col++; gc.weightx = 0;
        filters.add(new JLabel("Title-Regex:"), gc);
        gc.gridx = col++; gc.gridwidth = 3; gc.weightx = 0.8;
        filters.add(cbTitleRegex, gc);
        gc.gridwidth = 1;

        // Row 2: Message-Regex (own line)
        row++; col = 0;
        gc.gridy = row; gc.gridx = col++; gc.weightx = 0;
        filters.add(new JLabel("Message-Regex:"), gc);
        gc.gridx = col++; gc.gridwidth = 3; gc.weightx = 0.8;
        filters.add(cbMessageRegex, gc);
        gc.gridwidth = 1;

        // Actions panel (right)
        JPanel actions = new JPanel(new GridBagLayout());
        actions.setBorder(new TitledBorder("Aktionen"));
        GridBagConstraints ac = new GridBagConstraints();
        ac.insets = new Insets(6, 6, 6, 6);
        ac.anchor = GridBagConstraints.NORTHWEST;
        ac.fill = GridBagConstraints.HORIZONTAL;
        ac.weightx = 1.0;

        int arow = 0;
        ac.gridx = 0; ac.gridy = arow++;
        actions.add(btnAwait, ac);
        ac.gridx = 0; ac.gridy = arow++;
        actions.add(btnAwaitAndGroups, ac);

        ac.gridx = 0; ac.gridy = arow++; ac.insets = new Insets(16, 6, 6, 6);
        actions.add(btnClear, ac);

        north.add(filters, BorderLayout.CENTER);
        north.add(actions, BorderLayout.EAST);
        return north;
    }

    private static void configureTable(JTable t) {
        t.setRowHeight(22);
        t.setFillsViewportHeight(true);
        t.setAutoCreateRowSorter(true);
        t.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

        if (t.getColumnModel().getColumnCount() >= 5) {
            t.getColumnModel().getColumn(0).setPreferredWidth(110);  // Zeit
            t.getColumnModel().getColumn(1).setPreferredWidth(70);   // Type
            t.getColumnModel().getColumn(2).setPreferredWidth(240);  // Title
            t.getColumnModel().getColumn(3).setPreferredWidth(560);  // Message
            t.getColumnModel().getColumn(4).setPreferredWidth(160);  // Context
        }
    }

    private void installEscToClose() {
        getRootPane().registerKeyboardAction(
                e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        getRootPane().setDefaultButton(btnAwait);
    }

    // ---------- Service wiring ----------

    /** Resolve NotificationService for the active page and prime the table. */
    private void resolveServiceForActivePage() {
        PageImpl active = BrowserServiceImpl.getInstance().getBrowser().getActivePage();
        if (active == null) {
            JOptionPane.showMessageDialog(this, "Keine aktive Page verfügbar.", "Hinweis", JOptionPane.WARNING_MESSAGE);
            return;
        }
        service = NotificationService.getInstance(active);
        List<GrowlNotification> all = service.getAll();
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() { refill(all); }
        });
    }

    /** Subscribe as snapshot listener and auto-unsubscribe on close. */
    private void hookLiveUpdates() {
        if (service == null) return;
        listenerRef = new Consumer<List<GrowlNotification>>() {
            @Override public void accept(final List<GrowlNotification> snapshot) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override public void run() { refill(snapshot); }
                });
            }
        };
        service.addListener(listenerRef);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent e) {
                if (service != null && listenerRef != null) service.removeListener(listenerRef);
            }
        });
    }

    // ---------- Data binding ----------

    private void refill(List<GrowlNotification> list) {
        model.setRowCount(0);
        for (GrowlNotification n : list) {
            model.addRow(new Object[]{
                    TS_FMT.format(new java.util.Date(n.timestamp)),
                    n.type, n.title, n.message, n.contextId
            });
        }
        lbStatus.setText("Meldungen: " + list.size());
        if (model.getRowCount() > 0) {
            int last = model.getRowCount() - 1;
            table.getSelectionModel().setSelectionInterval(last, last);
            table.scrollRectToVisible(table.getCellRect(last, 0, true));
        }
    }

    // ---------- Await flow (runs in background) ----------

    private void doAwait(final boolean extractGroups) {
        String sev = (String) cbSeverity.getSelectedItem();
        if ("ANY".equals(sev)) sev = null;

        final String titleRx = normalizeRegex(getRegexFrom(cbTitleRegex));
        final String msgRx   = normalizeRegex(getRegexFrom(cbMessageRegex));

        final double seconds = ((Number) spTimeoutSeconds.getValue()).doubleValue();
        final long timeoutMs = Math.max(0L, (long) Math.floor(seconds * 1000.0d));

        btnAwait.setEnabled(false);
        btnAwaitAndGroups.setEnabled(false);

        String status = "Warte auf Notification – "
                + "severity=" + (sev == null ? "ANY" : sev)
                + ", titleRx=" + (titleRx == null ? "∅" : "/" + titleRx + "/")
                + ", msgRx="   + (msgRx   == null ? "∅" : "/" + msgRx   + "/")
                + ", timeout=" + seconds + "s";
        lbStatus.setText(status);

        final String finalSev = sev;
        new SwingWorker<Object, Void>() {
            @Override protected Object doInBackground() throws Exception {
                if (extractGroups) {
                    String rx = (msgRx != null ? msgRx : "(.+)");
                    // Expect a List<String> with all capture groups from message
                    return ToolsRegistry.getInstance()
                            .notificationTool()
                            .awaitAndExtractAllGroups(rx, timeoutMs);
                } else {
                    return ToolsRegistry.getInstance()
                            .notificationTool()
                            .await(finalSev, titleRx, msgRx, timeoutMs);
                }
            }
            @Override protected void done() {
                btnAwait.setEnabled(true);
                btnAwaitAndGroups.setEnabled(true);
                try {
                    Object res = get();
                    if (extractGroups) {
                        @SuppressWarnings("unchecked")
                        List<String> groups = (List<String>) res;
                        StringBuilder sb = new StringBuilder();
                        if (groups == null || groups.isEmpty()) {
                            sb.append("Keine Capture-Gruppen gefunden.");
                        } else {
                            for (int i = 0; i < groups.size(); i++) {
                                sb.append("#").append(i + 1).append(": ").append(groups.get(i) == null ? "null" : groups.get(i)).append("\n");
                            }
                        }
                        JOptionPane.showMessageDialog(NotificationTestDialog.this,
                                sb.toString().trim(),
                                "Await + groups",
                                JOptionPane.INFORMATION_MESSAGE);
                        lbStatus.setText("Await + groups beendet.");
                    } else {
                        GrowlNotification n = (GrowlNotification) res;
                        JOptionPane.showMessageDialog(NotificationTestDialog.this,
                                "[" + n.type + "] " + n.title + "\n" + n.message,
                                "Await: Treffer", mapSwingType(n.type));
                        lbStatus.setText("Await beendet.");
                    }
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause();
                    if (cause instanceof TimeoutException) {
                        JOptionPane.showMessageDialog(NotificationTestDialog.this,
                                "Timeout ohne Treffer.", "Await", JOptionPane.WARNING_MESSAGE);
                        lbStatus.setText("Timeout.");
                    } else {
                        JOptionPane.showMessageDialog(NotificationTestDialog.this,
                                "Fehler: " + (cause == null ? ex.getMessage() : cause.getMessage()),
                                "Await", JOptionPane.ERROR_MESSAGE);
                        lbStatus.setText("Fehler.");
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    lbStatus.setText("Abgebrochen.");
                }
            }
        }.execute();
    }

    // ---------- Utilities ----------

    /** Create an editable regex combo with provided presets and preferred editor width. */
    private static JComboBox<String> createEditableRegexCombo(java.util.List<String> presets, int columns) {
        java.util.List<String> src = (presets == null || presets.isEmpty())
                ? java.util.Arrays.asList(".*")
                : presets;

        JComboBox<String> cb = new JComboBox<String>(src.toArray(new String[src.size()]));
        cb.setEditable(true);
        Component editorComp = cb.getEditor().getEditorComponent();
        if (editorComp instanceof JTextField) {
            ((JTextField) editorComp).setColumns(columns);
        }
        // Keep explicit editor to avoid focus-based resets
        cb.setEditor(new BasicComboBoxEditor());
        return cb;
    }

    /** Read the current text from an editable combo (or selected item). */
    private static String getRegexFrom(JComboBox<String> cb) {
        Object sel = cb.getEditor().getItem();
        if (sel == null) {
            Object s = cb.getSelectedItem();
            return s == null ? null : String.valueOf(s);
        }
        String s = String.valueOf(sel);
        return (s != null && s.trim().length() > 0) ? s : null;
    }

    private static int mapSwingType(String sev) {
        String s = sev == null ? "" : sev.toUpperCase();
        if (s.startsWith("WARN")) return JOptionPane.WARNING_MESSAGE;
        if (s.startsWith("ERROR") || s.startsWith("FATAL")) return JOptionPane.ERROR_MESSAGE;
        return JOptionPane.INFORMATION_MESSAGE;
    }

    private static String emptyToNull(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }

    private static String normalizeRegex(String s) {
        // Trim and turn empty into null → match-all semantics
        return emptyToNull(s);
    }
}
