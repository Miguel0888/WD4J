package de.bund.zrb.ui.commands;

import de.bund.zrb.config.InputDelaysConfig;
import de.bund.zrb.service.SettingsService;
import de.bund.zrb.ui.commandframework.ShortcutMenuCommand;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class SettingsCommand extends ShortcutMenuCommand {

    private static final double DEFAULT_WS_TIMEOUT_MS = 30_000.0;
    private static final int DEFAULT_KEY_DOWN_MS = 10;
    private static final int DEFAULT_KEY_UP_MS   = 30;

    private JSpinner spWsTimeout;
    private JTextField tfReportDir;
    private JCheckBox cbContextMode;
    private JSpinner spKeyDown;
    private JSpinner spKeyUp;

    private JDialog dialog;

    @Override
    public String getId() { return "file.configure"; }

    @Override
    public String getLabel() { return "Einstellungen..."; }

    @Override
    public void perform() {
        Double wsTimeout   = SettingsService.getInstance().get("websocketTimeout", Double.class);
        String reportDir   = SettingsService.getInstance().get("reportBaseDir", String.class);
        Boolean ctxMode    = SettingsService.getInstance().get("recording.contextMode", Boolean.class);
        Integer kdDelay    = SettingsService.getInstance().get("input.keyDownDelayMs", Integer.class);
        Integer kuDelay    = SettingsService.getInstance().get("input.keyUpDelayMs", Integer.class);

        double initialWsTimeout = wsTimeout != null ? wsTimeout : DEFAULT_WS_TIMEOUT_MS;
        String initialReportDir = (reportDir != null && !reportDir.trim().isEmpty()) ? reportDir : "C:/Reports";
        boolean initialCtxMode  = ctxMode != null ? ctxMode : true;
        int initialKd           = kdDelay != null ? kdDelay : DEFAULT_KEY_DOWN_MS;
        int initialKu           = kuDelay != null ? kuDelay : DEFAULT_KEY_UP_MS;

        dialog = new JDialog((Frame) null, "Einstellungen", true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setContentPane(buildContentPanel(initialWsTimeout, initialReportDir, initialCtxMode, initialKd, initialKu));
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    private JPanel buildContentPanel(double wsTimeout, String reportDir, boolean contextMode, int kd, int ku) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(new EmptyBorder(10, 12, 10, 12));

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        // --- Recording ---
        JPanel pnlRecording = new JPanel(new GridBagLayout());
        pnlRecording.setBorder(sectionBorder("Recording"));
        GridBagConstraints g1 = gbc();

        cbContextMode = new JCheckBox("Recording im Context-Mode (ein BrowserContext pro Benutzer)");
        cbContextMode.setSelected(contextMode);
        cbContextMode.setToolTipText("Aktiviert: Pro Benutzer wird ein eigener BrowserContext verwendet.");
        g1.gridx = 0; g1.gridy = 0; g1.gridwidth = 2; g1.weightx = 1; g1.anchor = GridBagConstraints.WEST;
        pnlRecording.add(cbContextMode, g1);

        // --- Eingabe ---
        JPanel pnlInput = new JPanel(new GridBagLayout());
        pnlInput.setBorder(sectionBorder("Eingabe (Keyboard)"));
        GridBagConstraints g2 = gbc();

        spKeyDown = new JSpinner(new SpinnerNumberModel(kd, 0, 2000, 1));
        spKeyUp   = new JSpinner(new SpinnerNumberModel(ku, 0, 2000, 1));
        Dimension spSz = new Dimension(120, 26);
        spKeyDown.setPreferredSize(spSz);
        spKeyUp.setPreferredSize(spSz);

        JLabel lbKd = new JLabel("KeyDown-Delay (ms):");
        JLabel lbKu = new JLabel("KeyUp-Delay (ms):");
        lbKd.setToolTipText("Wartezeit nach keyDown – hilfreich bei Masken/Formattern.");
        lbKu.setToolTipText("Wartezeit nach keyUp – hilfreich bei Masken/Formattern.");

        g2.gridx = 0; g2.gridy = 0; g2.anchor = GridBagConstraints.WEST;
        pnlInput.add(lbKd, g2);
        g2.gridx = 1; g2.gridy = 0; g2.anchor = GridBagConstraints.EAST; g2.weightx = 1;
        pnlInput.add(spKeyDown, g2);

        g2.gridx = 0; g2.gridy = 1; g2.anchor = GridBagConstraints.WEST; g2.weightx = 0;
        pnlInput.add(lbKu, g2);
        g2.gridx = 1; g2.gridy = 1; g2.anchor = GridBagConstraints.EAST; g2.weightx = 1;
        pnlInput.add(spKeyUp, g2);

        // --- Netzwerk ---
        JPanel pnlNet = new JPanel(new GridBagLayout());
        pnlNet.setBorder(sectionBorder("Netzwerk"));
        GridBagConstraints g3 = gbc();

        spWsTimeout = new JSpinner(new SpinnerNumberModel(wsTimeout, 0.0, Double.MAX_VALUE, 100.0));
        ((JSpinner.NumberEditor) spWsTimeout.getEditor()).getFormat().setGroupingUsed(false);
        spWsTimeout.setPreferredSize(spSz);
        JLabel lbWs = new JLabel("WebSocket Timeout (ms):");
        lbWs.setToolTipText("Maximale Wartezeit auf WebSocket-Operationen.");

        g3.gridx = 0; g3.gridy = 0; g3.anchor = GridBagConstraints.WEST;
        pnlNet.add(lbWs, g3);
        g3.gridx = 1; g3.gridy = 0; g3.anchor = GridBagConstraints.EAST; g3.weightx = 1;
        pnlNet.add(spWsTimeout, g3);

        // --- Reporting ---
        JPanel pnlReport = new JPanel(new GridBagLayout());
        pnlReport.setBorder(sectionBorder("Reporting"));
        GridBagConstraints g4 = gbc();

        tfReportDir = new JTextField(reportDir, 28);
        tfReportDir.setToolTipText("Basisverzeichnis für Reports.");

        JButton btBrowse = new JButton("Durchsuchen…");
        btBrowse.setMargin(new Insets(2, 10, 2, 10));
        btBrowse.setFocusable(false);
        btBrowse.addActionListener(e -> chooseReportDir());

        JLabel lbReport = new JLabel("Report-Verzeichnis:");

        // Zeile 1
        g4.gridx = 0; g4.gridy = 0; g4.anchor = GridBagConstraints.WEST; g4.weightx = 0;
        pnlReport.add(lbReport, g4);
        g4.gridx = 1; g4.gridy = 0; g4.fill = GridBagConstraints.HORIZONTAL; g4.weightx = 1;
        pnlReport.add(tfReportDir, g4);
        g4.gridx = 2; g4.gridy = 0; g4.fill = GridBagConstraints.NONE; g4.weightx = 0;
        pnlReport.add(btBrowse, g4);

        // zusammenbauen
        form.add(pnlRecording);
        form.add(Box.createVerticalStrut(8));
        form.add(pnlInput);
        form.add(Box.createVerticalStrut(8));
        form.add(pnlNet);
        form.add(Box.createVerticalStrut(8));
        form.add(pnlReport);

        // --- Button-Leiste unten: links App-Ordner öffnen, rechts Apply/OK/Cancel ---
        JPanel footer = new JPanel(new BorderLayout());

        JButton btOpenAppFolder = new JButton("App-Ordner öffnen");
        btOpenAppFolder.setToolTipText("Öffnet den Einstellungsordner (.wd4j) im Explorer.");
        btOpenAppFolder.setFocusable(false);
        btOpenAppFolder.addActionListener(e -> openAppFolder());

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.add(btOpenAppFolder);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton btApply = new JButton("Übernehmen");
        JButton btOk    = new JButton("OK");
        JButton btCancel= new JButton("Abbrechen");

        btApply.addActionListener(e -> applySettings(false));
        btOk.addActionListener(e -> applySettings(true));
        btCancel.addActionListener(e -> dialog.dispose());

        right.add(btApply);
        right.add(btOk);
        right.add(btCancel);

        footer.add(left, BorderLayout.WEST);
        footer.add(right, BorderLayout.EAST);

        root.add(form, BorderLayout.CENTER);
        root.add(Box.createVerticalStrut(10), BorderLayout.NORTH);
        root.add(footer, BorderLayout.SOUTH);
        return root;
    }

    private void chooseReportDir() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        File preset = new File(tfReportDir.getText().trim());
        if (preset.exists()) {
            chooser.setCurrentDirectory(preset.isDirectory() ? preset : preset.getParentFile());
        }
        int res = chooser.showOpenDialog(dialog);
        if (res == JFileChooser.APPROVE_OPTION) {
            tfReportDir.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void openAppFolder() {
        try {
            Path base = SettingsService.getInstance().getBasePath(); // <- JSON-Settings-Ordner
            if (base == null) {
                error("Basisordner konnte nicht ermittelt werden.");
                return;
            }
            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("win")) {
                new ProcessBuilder("explorer.exe", base.toAbsolutePath().toString()).start();
            } else if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(base.toFile());
            } else {
                error("Öffnen des Ordners wird auf diesem System nicht unterstützt.");
            }
        } catch (Exception ex) {
            error("Konnte Ordner nicht öffnen:\n" + ex.getMessage());
        }
    }

    private void applySettings(boolean closeAfter) {
        double timeoutValue = ((Number) spWsTimeout.getValue()).doubleValue();
        if (timeoutValue < 0) { error("Timeout darf nicht negativ sein."); return; }

        int kdVal = ((Number) spKeyDown.getValue()).intValue();
        int kuVal = ((Number) spKeyUp.getValue()).intValue();
        if (kdVal < 0 || kuVal < 0) { error("Delays dürfen nicht negativ sein."); return; }

        String reportDir = tfReportDir.getText().trim();
        if (reportDir.isEmpty()) { error("Bitte ein Report-Verzeichnis angeben."); return; }

        Map<String, Object> s = new HashMap<>();
        s.put("websocketTimeout", timeoutValue);
        s.put("reportBaseDir", reportDir);
        s.put("recording.contextMode", cbContextMode.isSelected());
        s.put("input.keyDownDelayMs", kdVal);
        s.put("input.keyUpDelayMs",   kuVal);
        s.forEach(SettingsService.getInstance()::set);

        // Live übernehmen
        InputDelaysConfig.setKeyDownDelayMs(kdVal);
        InputDelaysConfig.setKeyUpDelayMs(kuVal);

        if (closeAfter) dialog.dispose();
    }

    private void error(String msg) {
        JOptionPane.showMessageDialog(dialog, msg, "Eingabefehler", JOptionPane.ERROR_MESSAGE);
    }

    private static GridBagConstraints gbc() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.NONE;
        return gbc;
    }

    private static TitledBorder sectionBorder(String title) {
        TitledBorder tb = BorderFactory.createTitledBorder(title);
        tb.setTitleFont(tb.getTitleFont().deriveFont(Font.BOLD));
        return tb;
    }
}
