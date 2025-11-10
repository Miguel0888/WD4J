package de.bund.zrb.ui.commands;

import de.bund.zrb.config.InputDelaysConfig;
import de.bund.zrb.config.VideoConfig;
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
    // Assertion wait defaults (ms)
    private static final int DEFAULT_ASSERT_GROUP_WAIT_MS = 3000; // global wait before group
    private static final int DEFAULT_ASSERT_EACH_WAIT_MS  = 0;    // per-assertion wait

    // Video defaults
    private static final boolean DEFAULT_VIDEO_ENABLED = false;
    private static final int DEFAULT_VIDEO_FPS = 15;

    private JSpinner spWsTimeout;
    private JTextField tfReportDir;
    private JCheckBox cbContextMode;
    private JSpinner spKeyDown;
    private JSpinner spKeyUp;
    private JSpinner spAssertionGroupWait;
    private JSpinner spAssertionEachWait;
    private JSpinner spBeforeEachAfterWait; // NEU: Wartezeit nach jedem BeforeEach (Sekunden)

    // Video
    private JCheckBox cbVideoEnabled;
    private JSpinner  spVideoFps;
    private JTextField tfVideoDir;     // <- NEU: eigener Aufnahmepfad
    private JButton    btBrowseVideo;  // <- NEU

    private JDialog dialog;

    @Override
    public String getId() { return "file.configure"; }

    @Override
    public String getLabel() { return "Einstellungen..."; }

    @Override
    public void perform() {
        // bestehend
        Double  wsTimeout   = SettingsService.getInstance().get("websocketTimeout", Double.class);
        String  reportDir   = SettingsService.getInstance().get("reportBaseDir", String.class);
        Boolean ctxMode     = SettingsService.getInstance().get("recording.context.mode", Boolean.class);
        Integer kdDelay     = SettingsService.getInstance().get("input.keyDownDelayMs", Integer.class);
        Integer kuDelay     = SettingsService.getInstance().get("input.keyUpDelayMs", Integer.class);

        // neu: Video
        Boolean videoEnabled = SettingsService.getInstance().get("video.enabled", Boolean.class);
        Integer videoFps     = SettingsService.getInstance().get("video.fps", Integer.class);
        String  videoDir     = SettingsService.getInstance().get("video.reportsDir", String.class);

        Integer groupWaitMs = SettingsService.getInstance().get("assertion.groupWaitMs", Integer.class);
        Integer eachWaitMs  = SettingsService.getInstance().get("assertion.eachWaitMs", Integer.class);
        Integer beforeEachAfterMs = SettingsService.getInstance().get("beforeEach.afterWaitMs", Integer.class); // neu

        double  initialWsTimeout = wsTimeout != null ? wsTimeout : DEFAULT_WS_TIMEOUT_MS;
        String  initialReportDir = (reportDir != null && !reportDir.trim().isEmpty()) ? reportDir : "C:/Reports";
        boolean initialCtxMode   = ctxMode != null ? ctxMode : true;
        int     initialKd        = kdDelay != null ? kdDelay : DEFAULT_KEY_DOWN_MS;
        int     initialKu        = kuDelay != null ? kuDelay : DEFAULT_KEY_UP_MS;

        boolean initialVideoEnabled = videoEnabled != null ? videoEnabled : DEFAULT_VIDEO_ENABLED;
        int     initialVideoFps     = videoFps != null ? videoFps : DEFAULT_VIDEO_FPS;
        String  initialVideoDir     = (videoDir != null && !videoDir.trim().isEmpty())
                ? videoDir
                : initialReportDir; // Default: nimm Report-Ordner

        double initialAssertGroupWaitS = groupWaitMs != null ? groupWaitMs / 1000.0 : DEFAULT_ASSERT_GROUP_WAIT_MS / 1000.0;
        double initialAssertEachWaitS  = eachWaitMs  != null ? eachWaitMs  / 1000.0 : DEFAULT_ASSERT_EACH_WAIT_MS  / 1000.0;
        double initialBeforeEachAfterWaitS = beforeEachAfterMs != null ? beforeEachAfterMs / 1000.0 : 0.0; // Default 0

        dialog = new JDialog((Frame) null, "Einstellungen", true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setContentPane(buildContentPanel(
                initialWsTimeout,
                initialReportDir,
                initialCtxMode,
                initialKd,
                initialKu,
                initialVideoEnabled,
                initialVideoFps,
                initialVideoDir,
                initialAssertGroupWaitS,
                initialAssertEachWaitS,
                initialBeforeEachAfterWaitS // neu
        ));
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    private JPanel buildContentPanel(double wsTimeout,
                                     String reportDir,
                                     boolean contextMode,
                                     int kd,
                                     int ku,
                                     boolean videoEnabled,
                                     int videoFps,
                                     String videoDir,
                                     double assertionGroupWaitS,
                                     double assertionEachWaitS,
                                     double beforeEachAfterWaitS // neu
    ) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(new EmptyBorder(10, 12, 10, 12));

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        // --- Recording ---
        JPanel pnlRecording = new JPanel(new GridBagLayout());
        pnlRecording.setBorder(sectionBorder("Aufnahme"));
        GridBagConstraints g1 = gbc();

        int row = 0;

        cbContextMode = new JCheckBox("Recording im Context-Mode (ein BrowserContext pro Benutzer)");
        cbContextMode.setSelected(contextMode);
        cbContextMode.setToolTipText("Aktiviert: Pro Benutzer wird ein eigener BrowserContext verwendet.");
        g1.gridx = 0; g1.gridy = row++; g1.gridwidth = 3; g1.weightx = 1; g1.anchor = GridBagConstraints.WEST;
        pnlRecording.add(cbContextMode, g1);
        g1.gridwidth = 1;

        // Video-Optionen (Windows)
        cbVideoEnabled = new JCheckBox("Fenster-Video aufzeichnen (Windows)");
        cbVideoEnabled.setSelected(videoEnabled);
        cbVideoEnabled.setToolTipText("Zeichnet das Browserfenster auf (auch im Hintergrund), falls verfügbar.");
        g1.gridx = 0; g1.gridy = row++; g1.gridwidth = 3; g1.anchor = GridBagConstraints.WEST; g1.weightx = 1;
        pnlRecording.add(cbVideoEnabled, g1);
        g1.gridwidth = 1;

        JLabel lbFps = new JLabel("FPS:");
        spVideoFps = new JSpinner(new SpinnerNumberModel(videoFps, 1, 120, 1));
        Dimension spSz = new Dimension(120, 26);
        spVideoFps.setPreferredSize(spSz);
        lbFps.setToolTipText("Bilder pro Sekunde für die Videoaufzeichnung (z. B. 15).");
        g1.gridx = 0; g1.gridy = row; g1.anchor = GridBagConstraints.WEST; g1.weightx = 0;
        pnlRecording.add(lbFps, g1);
        g1.gridx = 1; g1.gridy = row++; g1.anchor = GridBagConstraints.EAST; g1.weightx = 1;
        pnlRecording.add(spVideoFps, g1);

        // 💡 Mini-Button für "Video-Details..."
        JButton btVideoDetails = new JButton(new String(Character.toChars(0x1F3AC))); // 🎬
        btVideoDetails.setToolTipText("Video-Details …");
        btVideoDetails.setFocusable(false);
        btVideoDetails.setMargin(new Insets(0, 0, 0, 0));
        Dimension sq = new Dimension(26, 26);
        btVideoDetails.setPreferredSize(sq);
        btVideoDetails.setMinimumSize(sq);
        btVideoDetails.setMaximumSize(sq);

        // Klick öffnet den Dialog
        btVideoDetails.addActionListener(e -> {
            Window owner = SwingUtilities.getWindowAncestor(dialog);
            new de.bund.zrb.ui.video.VideoSettingsDialog(owner).setVisible(true);
        });

        // Button neben FPS platzieren (gleiches Grid-Row wie FPS):
        g1.gridx = 2; g1.gridy = row-1; g1.anchor = GridBagConstraints.CENTER; g1.weightx = 0;
        pnlRecording.add(btVideoDetails, g1);

        // NEU: Video-Ordner
        JLabel lbVideoDir = new JLabel("Video-Ordner:");
        tfVideoDir = new JTextField(videoDir, 28);
        tfVideoDir.setToolTipText("Zielordner für Video-Dateien.");
        btBrowseVideo = new JButton("Durchsuchen…");
        btBrowseVideo.setMargin(new Insets(2, 10, 2, 10));
        btBrowseVideo.setFocusable(false);
        btBrowseVideo.addActionListener(e -> chooseDirInto(tfVideoDir));

        g1.gridx = 0; g1.gridy = row; g1.anchor = GridBagConstraints.WEST; g1.weightx = 0;
        pnlRecording.add(lbVideoDir, g1);
        g1.gridx = 1; g1.gridy = row; g1.fill = GridBagConstraints.HORIZONTAL; g1.weightx = 1;
        pnlRecording.add(tfVideoDir, g1);
        g1.gridx = 2; g1.gridy = row++; g1.fill = GridBagConstraints.NONE; g1.weightx = 0;
        pnlRecording.add(btBrowseVideo, g1);

        // --- Eingabe ---
        JPanel pnlInput = new JPanel(new GridBagLayout());
        pnlInput.setBorder(sectionBorder("Wiedergabe"));
        GridBagConstraints g2 = gbc();

        spKeyDown = new JSpinner(new SpinnerNumberModel(kd, 0, 2000, 1));
        spKeyUp   = new JSpinner(new SpinnerNumberModel(ku, 0, 2000, 1));
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

        // --- Assertion waits (UI in Sekunden, internally ms) ---
        JLabel lbAssertGroup = new JLabel("Wartezeit vor allen Assertions (s):");
        spAssertionGroupWait = new JSpinner(new SpinnerNumberModel(assertionGroupWaitS, 0.0, Double.MAX_VALUE, 1.0));
        spAssertionGroupWait.setPreferredSize(spSz);
        JSpinner.NumberEditor groupEditor = new JSpinner.NumberEditor(spAssertionGroupWait, "0.###");
        spAssertionGroupWait.setEditor(groupEditor);
        spAssertionGroupWait.setToolTipText("Globale Wartezeit vor der Auswertung aller Assertions (Sekunden). Schritt: 1 s.");

        JLabel lbAssertEach = new JLabel("Wartezeit zwischen Assertions (s):");
        spAssertionEachWait = new JSpinner(new SpinnerNumberModel(assertionEachWaitS, 0.0, Double.MAX_VALUE, 1.0));
        spAssertionEachWait.setPreferredSize(spSz);
        JSpinner.NumberEditor eachEditor = new JSpinner.NumberEditor(spAssertionEachWait, "0.###");
        spAssertionEachWait.setEditor(eachEditor);
        spAssertionEachWait.setToolTipText("Wartezeit zwischen einzelnen Assertions (Sekunden). Schritt: 1 s. Setze 0 für kein zusätzliches Warten.");

        JLabel lbBeforeEachAfter = new JLabel("Wartezeit nach BeforeEach (s):");
        spBeforeEachAfterWait = new JSpinner(new SpinnerNumberModel(beforeEachAfterWaitS, 0.0, Double.MAX_VALUE, 1.0));
        spBeforeEachAfterWait.setPreferredSize(spSz);
        JSpinner.NumberEditor beEditor = new JSpinner.NumberEditor(spBeforeEachAfterWait, "0.###");
        spBeforeEachAfterWait.setEditor(beEditor);
        spBeforeEachAfterWait.setToolTipText("Pause nach Auswertung jeder BeforeEach-Gruppe (Root/Suite/Case) in Sekunden.");

        g2.gridx = 0; g2.gridy = 2; g2.anchor = GridBagConstraints.WEST; g2.weightx = 0;
        pnlInput.add(lbAssertGroup, g2);
        g2.gridx = 1; g2.gridy = 2; g2.anchor = GridBagConstraints.EAST; g2.weightx = 1;
        pnlInput.add(spAssertionGroupWait, g2);

        g2.gridx = 0; g2.gridy = 3; g2.anchor = GridBagConstraints.WEST; g2.weightx = 0;
        pnlInput.add(lbAssertEach, g2);
        g2.gridx = 1; g2.gridy = 3; g2.anchor = GridBagConstraints.EAST; g2.weightx = 1;
        pnlInput.add(spAssertionEachWait, g2);

        g2.gridx = 0; g2.gridy = 4; g2.anchor = GridBagConstraints.WEST; g2.weightx = 0;
        pnlInput.add(lbBeforeEachAfter, g2);
        g2.gridx = 1; g2.gridy = 4; g2.anchor = GridBagConstraints.EAST; g2.weightx = 1;
        pnlInput.add(spBeforeEachAfterWait, g2);

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
        pnlReport.setBorder(sectionBorder("Bericht"));
        GridBagConstraints g4 = gbc();

        tfReportDir = new JTextField(reportDir, 28);
        tfReportDir.setToolTipText("Basisverzeichnis für Reports (Screenshots, Logs, etc.).");

        JButton btBrowse = new JButton("Durchsuchen…");
        btBrowse.setMargin(new Insets(2, 10, 2, 10));
        btBrowse.setFocusable(false);
        btBrowse.addActionListener(e -> chooseDirInto(tfReportDir));

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

    private void chooseDirInto(JTextField target) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        File preset = new File(target.getText().trim());
        if (preset.exists()) {
            chooser.setCurrentDirectory(preset.isDirectory() ? preset : preset.getParentFile());
        }
        int res = chooser.showOpenDialog(dialog);
        if (res == JFileChooser.APPROVE_OPTION) {
            target.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void openAppFolder() {
        try {
            Path base = SettingsService.getInstance().getBasePath(); // JSON-Settings-Ordner (.wd4j)
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

        double groupWaitS = ((Number) spAssertionGroupWait.getValue()).doubleValue();
        if (groupWaitS < 0) { error("Globale Wartezeit darf nicht negativ sein."); return; }
        int groupWaitMs = (int) Math.round(groupWaitS * 1000.0);

        double eachWaitS = ((Number) spAssertionEachWait.getValue()).doubleValue();
        if (eachWaitS < 0) { error("Wartezeit zwischen Assertions darf nicht negativ sein."); return; }
        int eachWaitMs = (int) Math.round(eachWaitS * 1000.0);

        double beforeEachAfterS = ((Number) spBeforeEachAfterWait.getValue()).doubleValue();
        if (beforeEachAfterS < 0) { error("Wartezeit nach BeforeEach darf nicht negativ sein."); return; }
        int beforeEachAfterMs = (int) Math.round(beforeEachAfterS * 1000.0);

        String reportDir = tfReportDir.getText().trim();
        if (reportDir.isEmpty()) { error("Bitte ein Report-Verzeichnis angeben."); return; }

        boolean videoEnabled = cbVideoEnabled.isSelected();
        int fps = ((Number) spVideoFps.getValue()).intValue();
        if (fps <= 0) { error("FPS muss > 0 sein."); return; }

        String videoDir = tfVideoDir.getText().trim();
        if (videoDir.isEmpty()) { error("Bitte einen Video-Ordner angeben."); return; }

        Map<String, Object> s = new HashMap<>();
        s.put("websocketTimeout", timeoutValue);
        s.put("reportBaseDir", reportDir);
        s.put("recording.contextMode", cbContextMode.isSelected());
        s.put("input.keyDownDelayMs", kdVal);
        s.put("input.keyUpDelayMs",   kuVal);
        // Assertion waits (ms)
        s.put("assertion.groupWaitMs", groupWaitMs);
        s.put("assertion.eachWaitMs", eachWaitMs);
        s.put("beforeEach.afterWaitMs", beforeEachAfterMs); // neu

        // Video-Settings persistieren
        s.put("video.enabled",   videoEnabled);
        s.put("video.fps",       fps);
        s.put("video.reportsDir", videoDir);

        s.forEach(SettingsService.getInstance()::set);

        // Live übernehmen (Adapter-Sicht)
        InputDelaysConfig.setKeyDownDelayMs(kdVal);
        InputDelaysConfig.setKeyUpDelayMs(kuVal);
        VideoConfig.setEnabled(videoEnabled);
        VideoConfig.setFps(fps);
        VideoConfig.setReportsDir(videoDir);
        // Keine separate Config-Klasse für assertion.waitMs vorhanden – Setting ist persistent via SettingsService

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
