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

    // Browser-Settings UI
    private JComboBox<String> cbBrowserType;
    private JSpinner spPort;
    private JCheckBox cbHeadless;
    private JCheckBox cbDisableGpu;
    private JCheckBox cbNoRemote;
    private JCheckBox cbStartMax;
    private JCheckBox cbUseProfile;
    private JTextField tfProfilePath;
    private JButton btBrowseProfile;
    private JTextField tfExtraArgs;
    private JCheckBox cbConfirmTerminateRunning;
    private JCheckBox cbDebugEnabled;
    private JCheckBox cbLogWebSocket;
    private JCheckBox cbLogVideo;
    private JCheckBox cbLogBrowser;
    private JCheckBox cbLogTabManager;
    private JCheckBox cbLogNetwork;
    private JCheckBox cbNetworkWaitEnabled;
    private JSpinner spNetworkWaitMs;

    // neu
    private JSpinner spCmdRetryCount; // neu
    private JSpinner spCmdRetryWindowS; // neu in Sekunden

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

        // Browser (neu)
        String  browserSel  = SettingsService.getInstance().get("browser.selected", String.class);
        Integer portVal     = SettingsService.getInstance().get("browser.port", Integer.class);
        Boolean headless    = SettingsService.getInstance().get("browser.headless", Boolean.class);
        Boolean disableGpu  = SettingsService.getInstance().get("browser.disableGpu", Boolean.class);
        Boolean noRemote    = SettingsService.getInstance().get("browser.noRemote", Boolean.class);
        Boolean startMax    = SettingsService.getInstance().get("browser.startMaximized", Boolean.class);
        Boolean useProfile  = SettingsService.getInstance().get("browser.useProfile", Boolean.class);
        String  profilePath = SettingsService.getInstance().get("browser.profilePath", String.class);
        String  extraArgs   = SettingsService.getInstance().get("browser.extraArgs", String.class);
        Boolean confirmTerminate = SettingsService.getInstance().get("browser.confirmTerminateRunning", Boolean.class);
        Boolean dbgEnabled       = SettingsService.getInstance().get("debug.enabled", Boolean.class);
        Boolean dbgWs            = SettingsService.getInstance().get("debug.websocket", Boolean.class);
        Boolean dbgVid           = SettingsService.getInstance().get("debug.video", Boolean.class);
        Boolean dbgBrowser       = SettingsService.getInstance().get("debug.browser", Boolean.class);
        Boolean dbgTabManager    = SettingsService.getInstance().get("debug.tabmanager", Boolean.class);
        Boolean dbgNetwork       = SettingsService.getInstance().get("debug.network", Boolean.class);
        Boolean netWaitEnabled  = SettingsService.getInstance().get("network.waitEnabled", Boolean.class);
        Long    netWaitMs       = SettingsService.getInstance().get("network.waitForCompleteMs", Long.class);

        Integer cmdRetryCount = SettingsService.getInstance().get("command.retry.maxCount", Integer.class);
        Long    cmdRetryWindow = SettingsService.getInstance().get("command.retry.windowMs", Long.class);
        double  initialCmdRetryWindowS = (cmdRetryWindow != null ? cmdRetryWindow : 0L) / 1000.0;
        int     initialCmdRetryCount   = cmdRetryCount != null ? cmdRetryCount : 0;

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

        // Browser defaults
        String initialBrowserSel = (browserSel != null && !browserSel.isEmpty()) ? browserSel : "firefox";
        int    initialPort       = portVal != null ? portVal : 9222;
        boolean initialHeadless  = headless != null ? headless : false;
        boolean initialDisable   = disableGpu != null ? disableGpu : false;
        boolean initialNoRemote  = noRemote != null ? noRemote : false;
        boolean initialStartMax  = startMax != null ? startMax : true;
        boolean initialUseProf   = useProfile != null ? useProfile : false;
        String  initialProfPath  = profilePath != null ? profilePath : "";
        String  initialExtraArgs = extraArgs != null ? extraArgs : "";
        boolean initialConfirmTerminate = confirmTerminate != null ? confirmTerminate : true;
        boolean initialDebugEnabled    = dbgEnabled != null ? dbgEnabled : false;
        boolean initialLogWs           = dbgWs != null ? dbgWs : false;
        boolean initialLogVideo        = dbgVid != null ? dbgVid : false;
        boolean initialLogBrowser      = dbgBrowser != null ? dbgBrowser : false;
        boolean initialLogTabManager   = dbgTabManager != null ? dbgTabManager : false;
        boolean initialLogNetwork      = dbgNetwork != null ? dbgNetwork : false;
        boolean initialNetWaitEnabled  = netWaitEnabled == null || netWaitEnabled.booleanValue();
        long    initialNetWaitMs       = (netWaitMs != null && netWaitMs >= 0) ? netWaitMs : 5000L;

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
                initialBeforeEachAfterWaitS,
                initialDebugEnabled,
                initialLogWs,
                initialLogVideo,
                initialLogBrowser,
                initialLogTabManager,
                initialLogNetwork,
                initialNetWaitEnabled,
                initialNetWaitMs,
                initialCmdRetryCount,
                initialCmdRetryWindowS
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
                                     double beforeEachAfterWaitS,
                                     boolean initialDebugEnabled,
                                     boolean initialLogWs,
                                     boolean initialLogVideo,
                                     boolean initialLogBrowser,
                                     boolean initialLogTabManager,
                                     boolean initialLogNetwork,
                                     boolean initialNetWaitEnabled,
                                     long initialNetWaitMs,
                                     int initialCmdRetryCount,
                                     double initialCmdRetryWindowS
    ) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(new EmptyBorder(10, 12, 10, 12));

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        // --- Browser ---
        JPanel pnlBrowser = new JPanel(new GridBagLayout());
        pnlBrowser.setBorder(sectionBorder("Browser"));
        GridBagConstraints gb = gbc();

        cbBrowserType = new JComboBox<>(new String[]{"firefox","chromium","edge"});
        // Lade initial aus Settings
        String sel = SettingsService.getInstance().get("browser.selected", String.class);
        cbBrowserType.setSelectedItem(sel != null ? sel.toLowerCase() : "firefox");

        spPort = new JSpinner(new SpinnerNumberModel(
                (Number) (SettingsService.getInstance().get("browser.port", Integer.class) != null ? SettingsService.getInstance().get("browser.port", Integer.class) : 9222),
                0, 65535, 1));
        Dimension spSz = new Dimension(120, 26);
        spPort.setPreferredSize(spSz);

        cbHeadless = new JCheckBox("Headless");
        cbHeadless.setSelected(Boolean.TRUE.equals(SettingsService.getInstance().get("browser.headless", Boolean.class)));
        cbDisableGpu = new JCheckBox("GPU deaktivieren");
        cbDisableGpu.setSelected(Boolean.TRUE.equals(SettingsService.getInstance().get("browser.disableGpu", Boolean.class)));
        cbNoRemote = new JCheckBox("No-Remote");
        cbNoRemote.setSelected(Boolean.TRUE.equals(SettingsService.getInstance().get("browser.noRemote", Boolean.class)));
        cbStartMax = new JCheckBox("Maximiert starten");
        Boolean sm = SettingsService.getInstance().get("browser.startMaximized", Boolean.class);
        cbStartMax.setSelected(sm != null ? sm : true);
        cbUseProfile = new JCheckBox("Profil verwenden");
        cbUseProfile.setSelected(Boolean.TRUE.equals(SettingsService.getInstance().get("browser.useProfile", Boolean.class)));
        tfProfilePath = new JTextField(SettingsService.getInstance().get("browser.profilePath", String.class) != null ? SettingsService.getInstance().get("browser.profilePath", String.class) : "", 24);
        btBrowseProfile = new JButton("Durchsuchen…");
        btBrowseProfile.setFocusable(false);
        btBrowseProfile.addActionListener(e -> chooseDirInto(tfProfilePath));
        tfExtraArgs = new JTextField(SettingsService.getInstance().get("browser.extraArgs", String.class) != null ? SettingsService.getInstance().get("browser.extraArgs", String.class) : "", 28);
        tfExtraArgs.setToolTipText("Zusätzliche Startargumente (mit Leerzeichen getrennt)");
        cbConfirmTerminateRunning = new JCheckBox("Vor Start laufende Browser-Instanzen beenden (nach Rückfrage)");
        {
            Boolean c = SettingsService.getInstance().get("browser.confirmTerminateRunning", Boolean.class);
            cbConfirmTerminateRunning.setSelected(c != null ? c : true);
        }
        cbDebugEnabled = new JCheckBox("[Debug]-Logs");
        {
            Boolean dbg = SettingsService.getInstance().get("debug.enabled", Boolean.class);
            cbDebugEnabled.setSelected(dbg != null && dbg);
        }
        cbLogWebSocket = new JCheckBox("[WebSocket]-Logs");
        cbLogWebSocket.setSelected(initialLogWs);
        cbLogVideo = new JCheckBox("[Video]-Logs");
        cbLogVideo.setSelected(initialLogVideo);
        cbLogBrowser = new JCheckBox("[" + String.valueOf(cbBrowserType.getSelectedItem()).toLowerCase() + "]-Logs");
        cbLogBrowser.setSelected(initialLogBrowser);
        cbLogTabManager = new JCheckBox("[TabManager]-Logs");
        cbLogTabManager.setSelected(initialLogTabManager);
        cbLogNetwork = new JCheckBox("[Playback]-Logs"); // renamed from [Network]-Logs
        cbLogNetwork.setSelected(initialLogNetwork);
        cbNetworkWaitEnabled = new JCheckBox("Netzwerk-Wartefunktion aktiv");
        cbNetworkWaitEnabled.setSelected(initialNetWaitEnabled);
        spNetworkWaitMs = new JSpinner(new SpinnerNumberModel(initialNetWaitMs, 0L, 120_000L, 100L));
        spNetworkWaitMs.setPreferredSize(new Dimension(120, 26));
        spNetworkWaitMs.setToolTipText("Timeout in Millisekunden für das Warten auf vollständige Antworten (0 = aus)");
        cbBrowserType.addItemListener(e -> {
            cbLogBrowser.setText("[" + String.valueOf(cbBrowserType.getSelectedItem()).toLowerCase() + "]-Logs");
        });

        int br = 0;
        gb.gridx = 0; gb.gridy = br; gb.anchor = GridBagConstraints.WEST; pnlBrowser.add(new JLabel("Browser:"), gb);
        gb.gridx = 1; gb.gridy = br++; gb.anchor = GridBagConstraints.EAST; pnlBrowser.add(cbBrowserType, gb);
        gb.gridx = 0; gb.gridy = br; gb.anchor = GridBagConstraints.WEST; pnlBrowser.add(new JLabel("Port:"), gb);
        gb.gridx = 1; gb.gridy = br++; gb.anchor = GridBagConstraints.EAST; pnlBrowser.add(spPort, gb);

        gb.gridx = 0; gb.gridy = br; gb.gridwidth = 2; gb.anchor = GridBagConstraints.WEST; pnlBrowser.add(cbHeadless, gb); br++;
        gb.gridx = 0; gb.gridy = br; gb.gridwidth = 2; pnlBrowser.add(cbDisableGpu, gb); br++;
        gb.gridx = 0; gb.gridy = br; gb.gridwidth = 2; pnlBrowser.add(cbNoRemote, gb); br++;
        gb.gridx = 0; gb.gridy = br; gb.gridwidth = 2; pnlBrowser.add(cbStartMax, gb); br++;
        gb.gridwidth = 1;

        gb.gridx = 0; gb.gridy = br; gb.anchor = GridBagConstraints.WEST; pnlBrowser.add(cbUseProfile, gb);
        gb.gridx = 1; gb.gridy = br++; gb.anchor = GridBagConstraints.EAST; pnlBrowser.add(new JLabel(""), gb);
        gb.gridx = 0; gb.gridy = br; gb.anchor = GridBagConstraints.WEST; pnlBrowser.add(new JLabel("Profilpfad:"), gb);
        gb.gridx = 1; gb.gridy = br++; gb.anchor = GridBagConstraints.EAST; pnlBrowser.add(tfProfilePath, gb);
        gb.gridx = 1; gb.gridy = br++; gb.anchor = GridBagConstraints.EAST; pnlBrowser.add(btBrowseProfile, gb);

        gb.gridx = 0; gb.gridy = br; gb.anchor = GridBagConstraints.WEST; pnlBrowser.add(new JLabel("Extra-Args:"), gb);
        gb.gridx = 1; gb.gridy = br++; gb.anchor = GridBagConstraints.EAST; pnlBrowser.add(tfExtraArgs, gb);
        gb.gridx = 0; gb.gridy = br; gb.gridwidth = 2; gb.anchor = GridBagConstraints.WEST; pnlBrowser.add(cbConfirmTerminateRunning, gb); br++;
        gb.gridwidth = 1;
        // Debug-Schalter werden in separatem Panel unten angezeigt

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
        Dimension spSz2 = new Dimension(120, 26);
        spVideoFps.setPreferredSize(spSz2);
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

        // Neue Retry-Felder
        JLabel lbRetryCount = new JLabel("Command Retry Count:");
        spCmdRetryCount = new JSpinner(new SpinnerNumberModel(initialCmdRetryCount, 0, 1000, 1));
        spCmdRetryCount.setPreferredSize(spSz);
        spCmdRetryCount.setToolTipText("Wie oft ein fehlgeschlagener Command erneut versucht wird (0 = aus).");
        g3.gridx = 0; g3.gridy = 1; g3.anchor = GridBagConstraints.WEST; g3.weightx = 0;
        pnlNet.add(lbRetryCount, g3);
        g3.gridx = 1; g3.gridy = 1; g3.anchor = GridBagConstraints.EAST; g3.weightx = 1;
        pnlNet.add(spCmdRetryCount, g3);

        JLabel lbRetryWindow = new JLabel("Retry-Zeitfenster (s):");
        spCmdRetryWindowS = new JSpinner(new SpinnerNumberModel(initialCmdRetryWindowS, 0.0, Double.MAX_VALUE, 1.0));
        JSpinner.NumberEditor rwEd = new JSpinner.NumberEditor(spCmdRetryWindowS, "0.###");
        spCmdRetryWindowS.setEditor(rwEd);
        spCmdRetryWindowS.setPreferredSize(spSz);
        spCmdRetryWindowS.setToolTipText("Maximale Dauer innerhalb der erneut versucht wird (Sekunden, 0 = aus).");
        g3.gridx = 0; g3.gridy = 2; g3.anchor = GridBagConstraints.WEST; g3.weightx = 0;
        pnlNet.add(lbRetryWindow, g3);
        g3.gridx = 1; g3.gridy = 2; g3.anchor = GridBagConstraints.EAST; g3.weightx = 1;
        pnlNet.add(spCmdRetryWindowS, g3);

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
        form.add(pnlBrowser);
        form.add(Box.createVerticalStrut(8));
        form.add(pnlRecording);
        form.add(Box.createVerticalStrut(8));
        form.add(pnlInput);
        form.add(Box.createVerticalStrut(8));
        form.add(pnlNet);
        form.add(Box.createVerticalStrut(8));
        form.add(pnlReport);
        form.add(Box.createVerticalStrut(8));

        // --- Debug / Terminal ---
        JPanel pnlDebug = new JPanel(new GridBagLayout());
        pnlDebug.setBorder(sectionBorder("Debug / Terminal"));
        GridBagConstraints gDbg = gbc();
        // Drei Spalten, zwei Reihen
        int drow = 0;
        // Reihe 1
        gDbg.gridx = 0; gDbg.gridy = drow; gDbg.anchor = GridBagConstraints.WEST; pnlDebug.add(cbDebugEnabled, gDbg);
        gDbg.gridx = 1; gDbg.gridy = drow; gDbg.anchor = GridBagConstraints.WEST; pnlDebug.add(cbLogWebSocket, gDbg);
        gDbg.gridx = 2; gDbg.gridy = drow++; gDbg.anchor = GridBagConstraints.WEST; pnlDebug.add(cbLogVideo, gDbg);
        // Reihe 2
        gDbg.gridx = 0; gDbg.gridy = drow; gDbg.anchor = GridBagConstraints.WEST; pnlDebug.add(cbLogBrowser, gDbg);
        gDbg.gridx = 1; gDbg.gridy = drow; gDbg.anchor = GridBagConstraints.WEST; pnlDebug.add(cbLogTabManager, gDbg);
        gDbg.gridx = 2; gDbg.gridy = drow++; gDbg.anchor = GridBagConstraints.WEST; pnlDebug.add(cbLogNetwork, gDbg);
        // Reihe 3: Netzwerk-Wait Settings (2 Spalten)
        gDbg.gridx = 0; gDbg.gridy = drow; gDbg.anchor = GridBagConstraints.WEST; pnlDebug.add(cbNetworkWaitEnabled, gDbg);
        gDbg.gridx = 1; gDbg.gridy = drow; gDbg.anchor = GridBagConstraints.WEST; pnlDebug.add(new JLabel("Timeout (ms):"), gDbg);
        gDbg.gridx = 2; gDbg.gridy = drow++; gDbg.anchor = GridBagConstraints.WEST; pnlDebug.add(spNetworkWaitMs, gDbg);

        form.add(pnlDebug);

        // --- Button-Leiste unten: links App-Ordner öffnen, rechts Apply/OK/Cancel ---
        JPanel footer = new JPanel(new BorderLayout());

        JButton btOpenAppFolder = new JButton("App-Ordner öffnen");
        btOpenAppFolder.setToolTipText("Öffnet den Einstellungsordner (.wd4j) im Explorer.");
        btOpenAppFolder.setFocusable(false);
        btOpenAppFolder.addActionListener(e -> openAppFolder());

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.add(btOpenAppFolder);
        JButton btSystem = new JButton("Systemeinstellungen");
        btSystem.setToolTipText("Browser-Pfade, Profile und WebSocket-Defaults konfigurieren");
        btSystem.setFocusable(false);
        btSystem.addActionListener(e -> {
            Window owner = SwingUtilities.getWindowAncestor(dialog);
            new de.bund.zrb.ui.system.BrowserSystemSettingsDialog(owner).setVisible(true);
        });
        left.add(btSystem);

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

        int cmdRetryCount = ((Number) spCmdRetryCount.getValue()).intValue();
        double cmdRetryWindowS = ((Number) spCmdRetryWindowS.getValue()).doubleValue();
        if (cmdRetryCount < 0) { error("Retry Count darf nicht negativ sein."); return; }
        if (cmdRetryWindowS < 0) { error("Retry-Zeitfenster darf nicht negativ sein."); return; }
        long cmdRetryWindowMs = Math.round(cmdRetryWindowS * 1000.0);

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

        // Browser persistieren
        s.put("browser.selected", String.valueOf(cbBrowserType.getSelectedItem()).toLowerCase());
        s.put("browser.port", ((Number) spPort.getValue()).intValue());
        s.put("browser.headless", cbHeadless.isSelected());
        s.put("browser.disableGpu", cbDisableGpu.isSelected());
        s.put("browser.noRemote", cbNoRemote.isSelected());
        s.put("browser.startMaximized", cbStartMax.isSelected());
        s.put("browser.useProfile", cbUseProfile.isSelected());
        s.put("browser.profilePath", tfProfilePath.getText().trim());
        s.put("browser.extraArgs", tfExtraArgs.getText().trim());
        s.put("browser.confirmTerminateRunning", cbConfirmTerminateRunning.isSelected());
        s.put("debug.enabled", cbDebugEnabled.isSelected());
        s.put("debug.websocket", cbLogWebSocket.isSelected());
        s.put("debug.video", cbLogVideo.isSelected());
        s.put("debug.browser", cbLogBrowser.isSelected());
        s.put("debug.tabmanager", cbLogTabManager.isSelected());
        s.put("debug.network", cbLogNetwork.isSelected());
        s.put("network.waitEnabled", cbNetworkWaitEnabled.isSelected());
        s.put("network.waitForCompleteMs", ((Number) spNetworkWaitMs.getValue()).longValue());
        s.put("command.retry.maxCount", cmdRetryCount);
        s.put("command.retry.windowMs", cmdRetryWindowMs);

        s.forEach(SettingsService.getInstance()::set);

        // System Properties sofort übernehmen (kein Neustart nötig)
        System.setProperty("wd4j.debug", String.valueOf(cbDebugEnabled.isSelected()));
        System.setProperty("wd4j.log.websocket", String.valueOf(cbLogWebSocket.isSelected()));
        System.setProperty("wd4j.log.video", String.valueOf(cbLogVideo.isSelected()));
        System.setProperty("wd4j.log.browser", String.valueOf(cbLogBrowser.isSelected()));
        System.setProperty("wd4j.log.tabmanager", String.valueOf(cbLogTabManager.isSelected()));
        System.setProperty("wd4j.log.network", String.valueOf(cbLogNetwork.isSelected()));
        System.setProperty("wd4j.command.retry.maxCount", String.valueOf(cmdRetryCount));
        System.setProperty("wd4j.command.retry.windowMs", String.valueOf(cmdRetryWindowMs));

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
