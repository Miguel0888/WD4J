package de.bund.zrb.ui.components;

import de.bund.zrb.PageImpl;
import de.bund.zrb.dto.GrowlNotification;
import de.bund.zrb.service.BrowserServiceImpl;
import de.bund.zrb.service.NotificationService;
import de.bund.zrb.service.ToolsRegistry;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public class NotificationTestDialog extends JDialog {

    private final JComboBox<String> cbSeverity;
    private final JTextField tfTitleRegex;
    private final JTextField tfMessageRegex;
    private final JSpinner spTimeoutMs;
    private final JSpinner spGroupIdx;
    private final JButton btnAwait;
    private final JButton btnAwaitAndExtract;

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
        setMinimumSize(new Dimension(1000, 520));
        setLocationRelativeTo(parent);

        // --- Top (Filter/Actions)
        JPanel top = new JPanel(new GridBagLayout());
        top.setBorder(new EmptyBorder(10,10,10,10));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4,4,4,4);
        gc.anchor = GridBagConstraints.WEST;

        cbSeverity = new JComboBox<>(new String[]{"ANY","INFO","WARN","ERROR","FATAL"});

        tfTitleRegex = new JTextField();
        tfTitleRegex.setColumns(24); // breiter
        tfMessageRegex = new JTextField();
        tfMessageRegex.setColumns(48); // noch breiter

        spTimeoutMs = new JSpinner(new SpinnerNumberModel(30_000, 100, 300_000, 500));
        spGroupIdx = new JSpinner(new SpinnerNumberModel(1, 0, 99, 1));
        btnAwait = new JButton("Await");
        btnAwaitAndExtract = new JButton("Await + extract group");

        int col = 0;
        // Zeile 1
        gc.gridy = 0;

        gc.gridx = col++;                          top.add(new JLabel("Severity:"), gc);
        gc.gridx = col++;                          top.add(cbSeverity, gc);

        gc.gridx = col++;                          top.add(new JLabel("Title-Regex:"), gc);
        gc.gridx = col; gc.gridwidth = 2;
        gc.fill = GridBagConstraints.HORIZONTAL; gc.weightx = 0.4;
        top.add(tfTitleRegex, gc);
        col += 2;

        gc.gridx = col++; gc.gridwidth = 1; gc.fill = GridBagConstraints.NONE; gc.weightx = 0;
        top.add(new JLabel("Timeout (ms):"), gc);
        gc.gridx = col++;                          top.add(spTimeoutMs, gc);

        // Zeile 2
        col = 0; gc.gridy = 1;

        gc.gridx = col++;                          top.add(btnAwait, gc);
        gc.gridx = col++;                          top.add(btnAwaitAndExtract, gc);

        gc.gridx = col++;                          top.add(new JLabel("Message-Regex:"), gc);
        gc.gridx = col; gc.gridwidth = 3;
        gc.fill = GridBagConstraints.HORIZONTAL; gc.weightx = 0.6;
        top.add(tfMessageRegex, gc);
        col += 3;

        gc.gridx = col++; gc.gridwidth = 1; gc.fill = GridBagConstraints.NONE; gc.weightx = 0;
        top.add(new JLabel("Group #"), gc);
        gc.gridx = col++;                          top.add(spGroupIdx, gc);

        // --- Center / Bottom wie gehabt ...
        model = new DefaultTableModel(COLS, 0) { @Override public boolean isCellEditable(int r, int c) { return false; } };
        table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        JScrollPane scroll = new JScrollPane(table);

        lbStatus = new JLabel("Bereit.");
        lbStatus.setBorder(new EmptyBorder(6,10,10,10));

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(top, BorderLayout.NORTH);
        getContentPane().add(scroll, BorderLayout.CENTER);
        getContentPane().add(lbStatus, BorderLayout.SOUTH);

        resolveServiceForActivePage();
        hookLiveUpdates();

        btnAwait.addActionListener(e -> doAwait(false));
        btnAwaitAndExtract.addActionListener(e -> doAwait(true));
    }

    // --- Resolve NotificationService für aktive Page
    private void resolveServiceForActivePage() {
        PageImpl active = BrowserServiceImpl.getInstance().getBrowser().getActivePage();
        if (active == null) {
            JOptionPane.showMessageDialog(this, "Keine aktive Page verfügbar.", "Hinweis", JOptionPane.WARNING_MESSAGE);
            return;
        }
        service = NotificationService.getInstance(active);
        // initiale Füllung
        List<GrowlNotification> all = service.getAll();
        SwingUtilities.invokeLater(() -> refill(all));
    }

    // --- Live-Listener registrieren
    private void hookLiveUpdates() {
        if (service == null) return;
        listenerRef = (snapshot) -> SwingUtilities.invokeLater(() -> refill(snapshot));
        service.addListener(listenerRef);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent e) {
                if (service != null && listenerRef != null) service.removeListener(listenerRef);
            }
        });
    }

    private void refill(List<GrowlNotification> list) {
        model.setRowCount(0);
        for (GrowlNotification n : list) {
            model.addRow(new Object[]{
                    TS_FMT.format(new java.util.Date(n.timestamp)),
                    n.type, n.title, n.message, n.contextId
            });
        }
        lbStatus.setText("Meldungen: " + list.size());
    }

    // --- Await-Aktionen (läuft im Worker)
    private void doAwait(boolean extractGroup) {
        // ANY → null; leere Felder → null
        String sev = (String) cbSeverity.getSelectedItem();
        if ("ANY".equals(sev)) sev = null;

        String titleRx = normalizeRegex(tfTitleRegex.getText());
        String msgRx   = normalizeRegex(tfMessageRegex.getText());
        long timeout   = ((Number) spTimeoutMs.getValue()).longValue();
        int groupIdx   = ((Number) spGroupIdx.getValue()).intValue();

        btnAwait.setEnabled(false);
        btnAwaitAndExtract.setEnabled(false);

        // Statusanzeige: was matchen wir gerade?
        String status = "Warte auf Notification – "
                + "severity=" + (sev == null ? "ANY" : sev)
                + ", titleRx=" + (titleRx == null ? "∅" : "/" + titleRx + "/")
                + ", msgRx="   + (msgRx   == null ? "∅" : "/" + msgRx   + "/")
                + ", timeout=" + timeout + "ms";
        lbStatus.setText(status);

        String finalSev = sev;
        new SwingWorker<Object, Void>() {
            @Override protected Object doInBackground() throws Exception {
                if (extractGroup) {
                    // Falls keine Message-Regex angegeben: alles matchen und Gruppe 1 extrahieren (meist sinnfrei),
                    // aber damit blockiert es nicht „unmöglich“:
                    String rx = (msgRx != null ? msgRx : "(.+)");
                    return de.bund.zrb.service.ToolsRegistry.getInstance()
                            .notificationTool()
                            .awaitAndExtractGroup(rx, groupIdx, timeout);
                } else {
                    return de.bund.zrb.service.ToolsRegistry.getInstance()
                            .notificationTool()
                            .await(finalSev, titleRx, msgRx, timeout);
                }
            }
            @Override protected void done() {
                btnAwait.setEnabled(true);
                btnAwaitAndExtract.setEnabled(true);
                try {
                    Object res = get();
                    if (extractGroup) {
                        String grp = (String) res;
                        JOptionPane.showMessageDialog(NotificationTestDialog.this,
                                (grp == null ? "Keine Capture-Group gefunden." : "Group #" + groupIdx + " = " + grp),
                                "Await + Extract", JOptionPane.INFORMATION_MESSAGE);
                        lbStatus.setText("Await + Extract beendet.");
                    } else {
                        GrowlNotification n = (GrowlNotification) res;
                        JOptionPane.showMessageDialog(NotificationTestDialog.this,
                                "[" + n.type + "] " + n.title + "\n" + n.message,
                                "Await: Treffer", mapSwingType(n.type));
                        lbStatus.setText("Await beendet.");
                        if (model.getRowCount() > 0) {
                            table.getSelectionModel().setSelectionInterval(model.getRowCount() - 1, model.getRowCount() - 1);
                            table.scrollRectToVisible(table.getCellRect(model.getRowCount() - 1, 0, true));
                        }
                    }
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause();
                    if (cause instanceof TimeoutException) {
                        JOptionPane.showMessageDialog(NotificationTestDialog.this,
                                "Timeout ohne Treffer.", "Await", JOptionPane.WARNING_MESSAGE);
                        lbStatus.setText("Timeout.");
                    } else {
                        JOptionPane.showMessageDialog(NotificationTestDialog.this,
                                "Fehler: " + cause.getMessage(), "Await", JOptionPane.ERROR_MESSAGE);
                        lbStatus.setText("Fehler.");
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    lbStatus.setText("Abgebrochen.");
                }
            }
        }.execute();
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
        // trims; komplett leer → null (→ match all)
        s = emptyToNull(s);
        return s;
    }
}
