package de.bund.zrb.ui.video;

import de.bund.zrb.config.VideoConfig;
import de.bund.zrb.service.SettingsService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;

public class VideoSettingsDialog extends JDialog {

    // --- FFmpeg Felder ---
    private JComboBox<String> cbContainer, cbCodec, cbPixFmt, cbQuality;
    private JCheckBox cbInterleaved, cbEvenDims;
    private JSpinner spThreads, spQscale, spCrf, spBitrate;
    private JTextField tfColorRange, tfColorSpace, tfColorTrc, tfColorPrim, tfPreset, tfTune, tfProfile, tfLevel;
    private JTextField tfVf, tfFallbacksCsv;
    private JTable tblExtra;
    private DefaultTableModel extraModel;

    // --- VLC Felder ---
    private JCheckBox cbVlcEnabled;
    private JCheckBox cbVlcAutodetect;
    private JTextField tfVlcBasePath;
    private JButton btVlcBaseBrowse;
    private JCheckBox cbVlcLogEnabled;
    private JTextField tfVlcLogPath;
    private JButton btVlcLogBrowse;
    private JSpinner spVlcVerbose;

    private JComboBox<String> cbVlcMux;
    private JTextField tfVlcVcodec;
    private JComboBox<String> cbVlcQuality;
    private JSpinner spVlcCrf;
    private JSpinner spVlcBitrate;
    private JCheckBox cbVlcDeint;
    private JTextField tfVlcDeintMode;
    private JTextField tfVlcVFilter;
    private JTextField tfVlcPreset;
    private JTextField tfVlcTune;
    private JTextField tfVlcSoutExtras;

    private JCheckBox cbVlcFullscreen;
    private JSpinner spVlcLeft, spVlcTop, spVlcWidth, spVlcHeight;
    private JCheckBox cbVlcAudioEnabled;

    private JComboBox<String> cbBackend;
    private JComboBox<String> cbJcodecContainer;
    private JCheckBox cbJcodecAudio;

    public VideoSettingsDialog(Window owner) {
        super(owner, "Video-Einstellungen", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(buildUI());
        pack();
        setLocationRelativeTo(owner);
        loadFromSettings();
        updateQualityCard();
        updateVlcUiEnabled();
    }

    // --- JCodec rudimentäre Felder
    private JPanel buildJcodecPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints g = gbc();
        int r = 0;
        JLabel info = new JLabel("JCodec (reines Java, kein Audio). Geeignet bei 32/64-bit-Mismatch.");
        g.gridx=0; g.gridy=r++; g.gridwidth=2; g.anchor=GridBagConstraints.WEST; p.add(info, g); g.gridwidth=1;

        cbJcodecContainer = new JComboBox<>(new String[]{"mp4","avi","mkv"});
        addRow(p, g, r++, "Container:", cbJcodecContainer);

        cbJcodecAudio = new JCheckBox("Experimentelles Audio (separat, Java-only)");
        g.gridx=0; g.gridy=r++; g.gridwidth=2; g.anchor=GridBagConstraints.WEST; p.add(cbJcodecAudio, g); g.gridwidth=1;

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.add(p, BorderLayout.NORTH);
        return wrap;
    }

    private JComponent buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(new EmptyBorder(10,12,10,12));

        // Backend-Auswahl oberhalb der Tabs
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        header.add(new JLabel("Backend:"));
        cbBackend = new JComboBox<>(new String[]{"vlc","ffmpeg","jcodec"});
        header.add(cbBackend);

        // Tabs
        JTabbedPane tabs = new JTabbedPane();
        // Reihenfolge geändert: JCodec, VLC, FFmpeg
        tabs.addTab("JCodec", buildJcodecPanel());
        tabs.addTab("VLC", buildVlcPanel());
        tabs.addTab("FFmpeg", buildFfmpegPanel());

        // Footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton btReset = new JButton("Defaults");
        JButton btOk    = new JButton("OK");
        JButton btCancel= new JButton("Abbrechen");
        btReset.addActionListener(e -> { loadDefaults(); updateVlcUiEnabled(); });
        btOk.addActionListener(e -> { if (save()) dispose(); });
        btCancel.addActionListener(e -> dispose());
        footer.add(btReset); footer.add(btOk); footer.add(btCancel);

        root.add(header, BorderLayout.NORTH);
        root.add(tabs, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);
        return root;
    }

    // --- FFmpeg Panel (bestehender Inhalt) ---
    private JPanel buildFfmpegPanel() {
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        // --- ENCODING ---
        JPanel pEnc = new JPanel(new GridBagLayout());
        pEnc.setBorder(section("Encoding / Format"));
        GridBagConstraints g = gbc();

        cbContainer = new JComboBox<>(new String[]{"matroska","mp4","avi","mov","ts"});
        cbCodec     = new JComboBox<>(new String[]{"mjpeg","libx264","libx265","h264","hevc"});
        cbPixFmt    = new JComboBox<>(new String[]{"yuv420p","yuv422p","yuv444p","rgb24","bgr24"});
        cbInterleaved = new JCheckBox("Interleaved");

        int r=0;
        addRow(pEnc, g, r++, "Container:", cbContainer);
        addRow(pEnc, g, r++, "Codec:", cbCodec);
        addRow(pEnc, g, r++, "Pixel-Format:", cbPixFmt);
        addRow(pEnc, g, r++, "", cbInterleaved);

        // --- QUALITY (Card: qscale/crf/bitrate) ---
        JPanel pQ = new JPanel(new GridBagLayout());
        pQ.setBorder(section("Qualität"));
        GridBagConstraints gq = gbc();
        cbQuality = new JComboBox<>(new String[]{"qscale","crf","bitrate"});
        spQscale  = new JSpinner(new SpinnerNumberModel(3, 1, 31, 1));
        spCrf     = new JSpinner(new SpinnerNumberModel(20, 0, 51, 1));
        spBitrate = new JSpinner(new SpinnerNumberModel(4000, 0, 200000, 100)); // kbps

        CardLayout qCards = new CardLayout();
        JPanel pnlCards = new JPanel(qCards);
        pnlCards.add(wrap(new JLabel("QScale (1=sehr gut .. 31=schlecht):"), spQscale), "qscale");
        pnlCards.add(wrap(new JLabel("CRF (x264/x265, 0..51):"), spCrf), "crf");
        pnlCards.add(wrap(new JLabel("Bitrate (kbps):"), spBitrate), "bitrate");

        cbQuality.addActionListener(e -> qCards.show(pnlCards, (String) cbQuality.getSelectedItem()));

        addRow(pQ, gq, 0, "Modus:", cbQuality);
        gq.gridx=0; gq.gridy=1; gq.gridwidth=2; gq.anchor=GridBagConstraints.WEST;
        pQ.add(pnlCards, gq); gq.gridwidth=1;

        // --- COLOR / FILTER ---
        JPanel pColor = new JPanel(new GridBagLayout());
        pColor.setBorder(section("Farbraum / Filter"));
        GridBagConstraints gc = gbc();

        tfColorRange = new JTextField(10);  tfColorRange.setToolTipText("pc|tv");
        tfColorSpace = new JTextField(10);  tfColorTrc   = new JTextField(10);
        tfColorPrim  = new JTextField(10);
        tfVf = new JTextField(36);
        addRow(pColor, gc, 0, "color_range:", tfColorRange);
        addRow(pColor, gc, 1, "colorspace:", tfColorSpace);
        addRow(pColor, gc, 2, "color_trc:",  tfColorTrc);
        addRow(pColor, gc, 3, "color_primaries:", tfColorPrim);
        addRow(pColor, gc, 4, "FFmpeg Filter (vf):", tfVf);

        // --- THREADS / DIMS / FALLBACKS ---
        JPanel pSys = new JPanel(new GridBagLayout());
        pSys.setBorder(section("System / Fallbacks"));
        GridBagConstraints gs = gbc();

        spThreads = new JSpinner(new SpinnerNumberModel(0, 0, 128, 1));
        cbEvenDims = new JCheckBox("Breite/Höhe auf gerade Werte erzwingen");
        tfFallbacksCsv = new JTextField(24); tfFallbacksCsv.setToolTipText("CSV, z. B. avi,mp4");
        addRow(pSys, gs, 0, "Threads:", spThreads);
        addRow(pSys, gs, 1, "", cbEvenDims);
        addRow(pSys, gs, 2, "Container-Fallbacks:", tfFallbacksCsv);

        // --- x264/x265 ---
        JPanel pX = new JPanel(new GridBagLayout());
        pX.setBorder(section("x264/x265 (optional)"));
        GridBagConstraints gx = gbc();

        tfPreset = new JTextField(10);
        tfTune   = new JTextField(10);
        tfProfile= new JTextField(10);
        tfLevel  = new JTextField(10);
        addRow(pX, gx, 0, "preset:", tfPreset);
        addRow(pX, gx, 1, "tune:",   tfTune);
        addRow(pX, gx, 2, "profile:",tfProfile);
        addRow(pX, gx, 3, "level:",  tfLevel);

        // --- Extra Video-Optionen (key=value) ---
        JPanel pExtra = new JPanel(new BorderLayout());
        pExtra.setBorder(section("Weitere FFmpeg-Videooptionen"));
        extraModel = new DefaultTableModel(new Object[]{"Schlüssel","Wert"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return true; }
        };
        tblExtra = new JTable(extraModel);
        tblExtra.setFillsViewportHeight(true);
        pExtra.add(new JScrollPane(tblExtra), BorderLayout.CENTER);
        JPanel pExtraBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4));
        JButton btAdd = new JButton("Neu");
        JButton btDel = new JButton("Löschen");
        btAdd.addActionListener(e -> extraModel.addRow(new Object[]{"",""}));
        btDel.addActionListener(e -> {
            int i = tblExtra.getSelectedRow();
            if (i >= 0) extraModel.removeRow(i);
        });
        pExtraBtns.add(btAdd); pExtraBtns.add(btDel);
        pExtra.add(pExtraBtns, BorderLayout.SOUTH);

        form.add(pEnc);      form.add(Box.createVerticalStrut(8));
        form.add(pQ);        form.add(Box.createVerticalStrut(8));
        form.add(pColor);    form.add(Box.createVerticalStrut(8));
        form.add(pSys);      form.add(Box.createVerticalStrut(8));
        form.add(pX);        form.add(Box.createVerticalStrut(8));
        form.add(pExtra);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(new JScrollPane(form), BorderLayout.CENTER);
        return wrapper;
    }

    // --- VLC Panel ---
    private JPanel buildVlcPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        // --- Backend & Pfad ---
        JPanel pTop = new JPanel(new GridBagLayout());
        GridBagConstraints g = gbc();
        int row = 0;
        cbVlcEnabled = new JCheckBox("VLC verwenden (sonst FFmpeg)");
        cbVlcEnabled.addActionListener(e -> updateVlcUiEnabled());
        g.gridx=0; g.gridy=row++; g.gridwidth=3; g.anchor=GridBagConstraints.WEST; g.weightx=1;
        pTop.add(cbVlcEnabled, g); g.gridwidth=1;

        cbVlcAutodetect = new JCheckBox("Autodetect (PATH/Registry)");
        cbVlcAutodetect.addActionListener(e -> updateVlcUiEnabled());
        g.gridx=0; g.gridy=row++; g.gridwidth=3; g.anchor=GridBagConstraints.WEST; g.weightx=1;
        pTop.add(cbVlcAutodetect, g); g.gridwidth=1;

        JLabel lbBase = new JLabel("VLC-Basispfad:");
        tfVlcBasePath = new JTextField(28);
        btVlcBaseBrowse = squareButton("…");
        btVlcBaseBrowse.setToolTipText("VLC-Ordner wählen (z. B. C:/Program Files/VideoLAN/VLC)");
        btVlcBaseBrowse.addActionListener(e -> chooseDirInto(tfVlcBasePath));
        g.gridx=0; g.gridy=row; g.anchor=GridBagConstraints.WEST; g.weightx=0;
        pTop.add(lbBase, g);
        g.gridx=1; g.gridy=row; g.fill=GridBagConstraints.HORIZONTAL; g.weightx=1;
        pTop.add(tfVlcBasePath, g);
        g.gridx=2; g.gridy=row++; g.fill=GridBagConstraints.NONE; g.weightx=0;
        pTop.add(btVlcBaseBrowse, g);

        cbVlcLogEnabled = new JCheckBox("VLC-Logdatei schreiben");
        cbVlcLogEnabled.addActionListener(e -> updateVlcUiEnabled());
        g.gridx=0; g.gridy=row++; g.gridwidth=3; g.anchor=GridBagConstraints.WEST; g.weightx=1;
        pTop.add(cbVlcLogEnabled, g); g.gridwidth=1;

        JLabel lbLog = new JLabel("Logdatei:");
        tfVlcLogPath = new JTextField(28);
        btVlcLogBrowse = squareButton("…");
        btVlcLogBrowse.setToolTipText("Logdatei wählen");
        btVlcLogBrowse.addActionListener(e -> chooseFileInto(tfVlcLogPath));
        g.gridx=0; g.gridy=row; g.anchor=GridBagConstraints.WEST; g.weightx=0;
        pTop.add(lbLog, g);
        g.gridx=1; g.gridy=row; g.fill=GridBagConstraints.HORIZONTAL; g.weightx=1;
        pTop.add(tfVlcLogPath, g);
        g.gridx=2; g.gridy=row++; g.fill=GridBagConstraints.NONE; g.weightx=0;
        pTop.add(btVlcLogBrowse, g);

        JLabel lbVerb = new JLabel("Verbose:");
        spVlcVerbose = new JSpinner(new SpinnerNumberModel(1, 0, 2, 1));
        g.gridx=0; g.gridy=row; g.anchor=GridBagConstraints.WEST; g.weightx=0; pTop.add(lbVerb, g);
        g.gridx=1; g.gridy=row++; g.anchor=GridBagConstraints.EAST; g.weightx=1; pTop.add(spVlcVerbose, g);

        p.add(pTop);
        p.add(Box.createVerticalStrut(8));

        // --- Aufnahme/Transcode ---
        JPanel pRec = new JPanel(new GridBagLayout());
        pRec.setBorder(section("Transcode / Ausgabe"));
        GridBagConstraints gt = gbc();
        int r = 0;
        cbVlcMux = new JComboBox<>(new String[]{"mp4","ts","mkv","avi"});
        tfVlcVcodec = new JTextField(10);
        cbVlcQuality = new JComboBox<>(new String[]{"crf","bitrate"});
        spVlcCrf = new JSpinner(new SpinnerNumberModel(23, 0, 51, 1));
        spVlcBitrate = new JSpinner(new SpinnerNumberModel(4000, 0, 200000, 100));
        cbVlcDeint = new JCheckBox("Deinterlace");
        tfVlcDeintMode = new JTextField(10);
        tfVlcVFilter = new JTextField(24);
        tfVlcPreset = new JTextField(10);
        tfVlcTune   = new JTextField(10);
        tfVlcSoutExtras = new JTextField(24);

        addRow(pRec, gt, r++, "Mux:", cbVlcMux);
        addRow(pRec, gt, r++, "vcodec:", tfVlcVcodec);
        addRow(pRec, gt, r++, "Qualität:", cbVlcQuality);
        addRow(pRec, gt, r++, "CRF:", spVlcCrf);
        addRow(pRec, gt, r++, "Bitrate (kbps):", spVlcBitrate);
        addRow(pRec, gt, r++, "Deinterlace:", cbVlcDeint);
        addRow(pRec, gt, r++, "Deint-Mode:", tfVlcDeintMode);
        addRow(pRec, gt, r++, "Video-Filter:", tfVlcVFilter);
        addRow(pRec, gt, r++, "x264/x265 preset:", tfVlcPreset);
        addRow(pRec, gt, r++, "x264/x265 tune:", tfVlcTune);
        addRow(pRec, gt, r++, "sout Extras:", tfVlcSoutExtras);

        p.add(pRec);
        p.add(Box.createVerticalStrut(8));

        // --- Screen-Region / Audio ---
        JPanel pSrc = new JPanel(new GridBagLayout());
        pSrc.setBorder(section("Quelle: screen:// Region / Audio"));
        GridBagConstraints gs = gbc();
        int srow = 0;
        cbVlcFullscreen = new JCheckBox("Voller Bildschirm");
        gs.gridx=0; gs.gridy=srow++; gs.gridwidth=3; gs.anchor=GridBagConstraints.WEST; pSrc.add(cbVlcFullscreen, gs); gs.gridwidth=1;
        spVlcLeft = new JSpinner(new SpinnerNumberModel(0, 0, 10000, 1));
        spVlcTop  = new JSpinner(new SpinnerNumberModel(0, 0, 10000, 1));
        spVlcWidth= new JSpinner(new SpinnerNumberModel(0, 0, 10000, 1));
        spVlcHeight=new JSpinner(new SpinnerNumberModel(0, 0, 10000, 1));
        addRow(pSrc, gs, srow++, "Left:", spVlcLeft);
        addRow(pSrc, gs, srow++, "Top:",  spVlcTop);
        addRow(pSrc, gs, srow++, "Width:",spVlcWidth);
        addRow(pSrc, gs, srow++, "Height:",spVlcHeight);
        cbVlcAudioEnabled = new JCheckBox("Audio mitschneiden (falls Quelle)");
        gs.gridx=0; gs.gridy=srow++; gs.gridwidth=3; gs.anchor=GridBagConstraints.WEST; pSrc.add(cbVlcAudioEnabled, gs); gs.gridwidth=1;

        p.add(pSrc);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(new JScrollPane(p), BorderLayout.CENTER);
        return wrapper;
    }

    private JButton squareButton(String text) {
        JButton b = new JButton(text);
        b.setMargin(new Insets(0,0,0,0));
        Dimension d = new Dimension(26, 26);
        b.setPreferredSize(d); b.setMinimumSize(d); b.setMaximumSize(d);
        b.setFocusable(false);
        return b;
    }

    private JPanel wrap(JComponent l, JComponent c) {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints g = gbc();
        g.gridx=0; g.gridy=0; g.anchor=GridBagConstraints.WEST;
        p.add(l, g);
        g.gridx=1; g.weightx=1; g.fill=GridBagConstraints.HORIZONTAL;
        p.add(c, g);
        return p;
    }

    private void addRow(JPanel p, GridBagConstraints g, int row, String label, JComponent comp) {
        g.gridx = 0; g.gridy = row; g.anchor = GridBagConstraints.WEST; g.weightx = 0;
        if (label != null && !label.isEmpty()) p.add(new JLabel(label), g);
        g.gridx = 1; g.gridy = row; g.anchor = GridBagConstraints.EAST; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        p.add(comp, g);
        g.fill = GridBagConstraints.NONE;
    }

    private GridBagConstraints gbc() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        return gbc;
    }
    private TitledBorder section(String title) {
        TitledBorder tb = BorderFactory.createTitledBorder(title);
        tb.setTitleFont(tb.getTitleFont().deriveFont(Font.BOLD));
        return tb;
    }

    // ---------- Load/Save ----------

    @SuppressWarnings("unchecked")
    private void loadFromSettings() {
        SettingsService s = SettingsService.getInstance();
        String backend = s.get("video.backend", String.class);
        if (backend == null || backend.trim().isEmpty()) backend = "vlc";
        cbBackend.setSelectedItem(backend);

        // FFmpeg
        sel(cbContainer,  s.get("video.container", String.class), "matroska");
        sel(cbCodec,      s.get("video.codec", String.class),     "mjpeg");
        sel(cbPixFmt,     s.get("video.pixfmt", String.class),    "yuv420p");
        cbInterleaved.setSelected(Boolean.TRUE.equals(s.get("video.interleaved", Boolean.class)));

        sel(cbQuality,    s.get("video.quality", String.class),   "qscale");
        setInt(spQscale,  or(s.get("video.qscale", Integer.class), 3));
        setInt(spCrf,     or(s.get("video.crf", Integer.class),   20));
        setInt(spBitrate, or(s.get("video.bitrateKbps", Integer.class), 0));

        tfColorRange.setText(or(s.get("video.color.range", String.class), "pc"));
        tfColorSpace.setText(or(s.get("video.color.space", String.class), "bt709"));
        tfColorTrc.setText(or(s.get("video.color.trc", String.class),     "bt709"));
        tfColorPrim.setText(or(s.get("video.color.primaries", String.class), "bt709"));

        tfVf.setText(or(s.get("video.vf", String.class), "scale=in_range=pc:out_range=pc,format=yuv420p"));

        setInt(spThreads, or(s.get("video.threads", Integer.class), 0));
        cbEvenDims.setSelected(Boolean.TRUE.equals(s.get("video.enforceEvenDims", Boolean.class)));

        List<String> fbs = s.get("video.container.fallbacks", List.class);
        tfFallbacksCsv.setText(fbs != null ? String.join(",", fbs) : "avi,mp4");

        tfPreset.setText(or(s.get("video.preset", String.class), null));
        tfTune.setText(or(s.get("video.tune", String.class), null));
        tfProfile.setText(or(s.get("video.profile", String.class), null));
        tfLevel.setText(or(s.get("video.level", String.class), null));

        Map<String,String> extra = s.get("video.ffopts", Map.class);
        extraModel.setRowCount(0);
        if (extra != null) {
            for (Map.Entry<String,String> e : extra.entrySet()) {
                extraModel.addRow(new Object[]{e.getKey(), e.getValue()});
            }
        }

        // VLC
        Boolean vlcEnabled = s.get("video.vlc.enabled", Boolean.class);
        cbVlcEnabled.setSelected(vlcEnabled == null || vlcEnabled);
        Boolean autod = s.get("video.vlc.autodetect", Boolean.class);
        cbVlcAutodetect.setSelected(Boolean.TRUE.equals(autod));
        tfVlcBasePath.setText(or(s.get("video.vlc.basePath", String.class), defaultVlcBasePath()));
        Boolean logE = s.get("video.vlc.log.enabled", Boolean.class);
        cbVlcLogEnabled.setSelected(Boolean.TRUE.equals(logE));
        tfVlcLogPath.setText(or(s.get("video.vlc.log.path", String.class), defaultVlcLogPath()));
        Integer verb = s.get("video.vlc.verbose", Integer.class);
        spVlcVerbose.setValue(verb == null ? 1 : Math.max(0, Math.min(2, verb)));

        sel(cbVlcMux, s.get("video.vlc.mux", String.class), "mp4");
        tfVlcVcodec.setText(or(s.get("video.vlc.vcodec", String.class), "h264"));
        sel(cbVlcQuality, s.get("video.vlc.quality", String.class), "crf");
        setInt(spVlcCrf,  or(s.get("video.vlc.crf", Integer.class), 23));
        setInt(spVlcBitrate, or(s.get("video.vlc.bitrateKbps", Integer.class), 4000));
        cbVlcDeint.setSelected(Boolean.TRUE.equals(s.get("video.vlc.deinterlace.enabled", Boolean.class)));
        tfVlcDeintMode.setText(or(s.get("video.vlc.deinterlace.mode", String.class), ""));
        tfVlcVFilter.setText(or(s.get("video.vlc.videoFilter", String.class), ""));
        tfVlcPreset.setText(or(s.get("video.vlc.venc.preset", String.class), ""));
        tfVlcTune.setText(or(s.get("video.vlc.venc.tune", String.class), ""));
        tfVlcSoutExtras.setText(or(s.get("video.vlc.soutExtras", String.class), ""));

        cbVlcFullscreen.setSelected(Boolean.TRUE.equals(s.get("video.vlc.screen.fullscreen", Boolean.class)));
        setInt(spVlcLeft,  or(s.get("video.vlc.screen.left", Integer.class), 0));
        setInt(spVlcTop,   or(s.get("video.vlc.screen.top", Integer.class), 0));
        setInt(spVlcWidth, or(s.get("video.vlc.screen.width", Integer.class), 0));
        setInt(spVlcHeight,or(s.get("video.vlc.screen.height", Integer.class), 0));
        cbVlcAudioEnabled.setSelected(Boolean.TRUE.equals(s.get("video.vlc.audio.enabled", Boolean.class)));

        // JCodec
        String jc = s.get("video.jcodec.container", String.class);
        if (jc == null || jc.trim().isEmpty()) jc = "mp4";
        if (cbJcodecContainer != null) cbJcodecContainer.setSelectedItem(jc);
        Boolean jAudio = s.get("video.jcodec.audio.enabled", Boolean.class);
        if (cbJcodecAudio != null) cbJcodecAudio.setSelected(Boolean.TRUE.equals(jAudio));

        updateQualityCard();
        updateVlcUiEnabled();
    }

    private void loadDefaults() {
        // FFmpeg Defaults
        cbContainer.setSelectedItem("matroska");
        cbCodec.setSelectedItem("mjpeg");
        cbPixFmt.setSelectedItem("yuv420p");
        cbInterleaved.setSelected(true);

        cbQuality.setSelectedItem("qscale");
        setInt(spQscale, 3);
        setInt(spCrf, 20);
        setInt(spBitrate, 0);

        tfColorRange.setText("pc");
        tfColorSpace.setText("bt709");
        tfColorTrc.setText("bt709");
        tfColorPrim.setText("bt709");
        tfVf.setText("scale=in_range=pc:out_range=pc,format=yuv420p");

        setInt(spThreads, 0);
        cbEvenDims.setSelected(true);
        tfFallbacksCsv.setText("avi,mp4");

        tfPreset.setText("");
        tfTune.setText("");
        tfProfile.setText("");
        tfLevel.setText("");

        extraModel.setRowCount(0);

        // VLC Defaults
        cbVlcEnabled.setSelected(true);
        cbVlcAutodetect.setSelected(false);
        tfVlcBasePath.setText(defaultVlcBasePath());
        cbVlcLogEnabled.setSelected(false);
        tfVlcLogPath.setText(defaultVlcLogPath());
        spVlcVerbose.setValue(1);

        cbVlcMux.setSelectedItem("mp4");
        tfVlcVcodec.setText("h264");
        cbVlcQuality.setSelectedItem("crf");
        setInt(spVlcCrf, 23);
        setInt(spVlcBitrate, 4000);
        cbVlcDeint.setSelected(false);
        tfVlcDeintMode.setText("");
        tfVlcVFilter.setText("");
        tfVlcPreset.setText("");
        tfVlcTune.setText("");
        tfVlcSoutExtras.setText("");

        cbVlcFullscreen.setSelected(false);
        setInt(spVlcLeft, 0);
        setInt(spVlcTop, 0);
        setInt(spVlcWidth, 0);
        setInt(spVlcHeight, 0);
        cbVlcAudioEnabled.setSelected(false);

        // JCodec Defaults
        if (cbJcodecContainer != null) cbJcodecContainer.setSelectedItem("mp4");
        if (cbJcodecAudio != null) cbJcodecAudio.setSelected(false);

        if (cbBackend != null) cbBackend.setSelectedItem("vlc");
    }

    private boolean save() {
        try {
            SettingsService s = SettingsService.getInstance();
            if (cbBackend != null) s.set("video.backend", String.valueOf(cbBackend.getSelectedItem()));
            if (cbJcodecContainer != null) s.set("video.jcodec.container", String.valueOf(cbJcodecContainer.getSelectedItem()));
            if (cbJcodecAudio != null) s.set("video.jcodec.audio.enabled", cbJcodecAudio.isSelected());

            // --- FFmpeg persistieren ---
            s.set("video.container",  str(cbContainer));
            s.set("video.codec",      str(cbCodec));
            s.set("video.pixfmt",     str(cbPixFmt));
            s.set("video.interleaved", cbInterleaved.isSelected());

            s.set("video.quality",    str(cbQuality));
            s.set("video.qscale",     ((Number) spQscale.getValue()).intValue());
            s.set("video.crf",        ((Number) spCrf.getValue()).intValue());
            s.set("video.bitrateKbps",((Number) spBitrate.getValue()).intValue());

            s.set("video.color.range",  clean(tfColorRange.getText()));
            s.set("video.color.space",  clean(tfColorSpace.getText()));
            s.set("video.color.trc",    clean(tfColorTrc.getText()));
            s.set("video.color.primaries", clean(tfColorPrim.getText()));
            s.set("video.vf",           clean(tfVf.getText()));

            s.set("video.threads",      ((Number) spThreads.getValue()).intValue());
            s.set("video.enforceEvenDims", cbEvenDims.isSelected());

            List<String> fbs = parseCsv(tfFallbacksCsv.getText());
            s.set("video.container.fallbacks", fbs);

            s.set("video.preset",  emptyToNull(tfPreset.getText()));
            s.set("video.tune",    emptyToNull(tfTune.getText()));
            s.set("video.profile", emptyToNull(tfProfile.getText()));
            s.set("video.level",   emptyToNull(tfLevel.getText()));

            Map<String,String> extra = new LinkedHashMap<>();
            for (int i=0; i<extraModel.getRowCount(); i++) {
                String k = clean(String.valueOf(extraModel.getValueAt(i,0)));
                String v = String.valueOf(extraModel.getValueAt(i,1));
                if (k != null && !k.isEmpty() && v != null) extra.put(k, v);
            }
            s.set("video.ffopts", extra);

            // --- VLC persistieren ---
            s.set("video.vlc.enabled", cbVlcEnabled.isSelected());
            s.set("video.vlc.autodetect", cbVlcAutodetect.isSelected());
            s.set("video.vlc.basePath", clean(tfVlcBasePath.getText()));
            s.set("video.vlc.log.enabled", cbVlcLogEnabled.isSelected());
            s.set("video.vlc.log.path", clean(tfVlcLogPath.getText()));
            s.set("video.vlc.verbose", ((Number) spVlcVerbose.getValue()).intValue());

            s.set("video.vlc.mux", str(cbVlcMux));
            s.set("video.vlc.vcodec", clean(tfVlcVcodec.getText()));
            s.set("video.vlc.quality", str(cbVlcQuality));
            s.set("video.vlc.crf", ((Number) spVlcCrf.getValue()).intValue());
            s.set("video.vlc.bitrateKbps", ((Number) spVlcBitrate.getValue()).intValue());
            s.set("video.vlc.deinterlace.enabled", cbVlcDeint.isSelected());
            s.set("video.vlc.deinterlace.mode", clean(tfVlcDeintMode.getText()));
            s.set("video.vlc.videoFilter", clean(tfVlcVFilter.getText()));
            s.set("video.vlc.venc.preset", emptyToNull(tfVlcPreset.getText()));
            s.set("video.vlc.venc.tune", emptyToNull(tfVlcTune.getText()));
            s.set("video.vlc.soutExtras", clean(tfVlcSoutExtras.getText()));

            s.set("video.vlc.screen.fullscreen", cbVlcFullscreen.isSelected());
            s.set("video.vlc.screen.left", ((Number) spVlcLeft.getValue()).intValue());
            s.set("video.vlc.screen.top", ((Number) spVlcTop.getValue()).intValue());
            s.set("video.vlc.screen.width", ((Number) spVlcWidth.getValue()).intValue());
            s.set("video.vlc.screen.height", ((Number) spVlcHeight.getValue()).intValue());
            s.set("video.vlc.audio.enabled", cbVlcAudioEnabled.isSelected());

            // Live in VideoConfig (FFmpeg) übernehmen
            VideoConfig.setContainer(str(cbContainer));
            VideoConfig.setCodec(str(cbCodec));
            VideoConfig.setPixelFmt(str(cbPixFmt));
            VideoConfig.setInterleaved(cbInterleaved.isSelected());

            VideoConfig.setQualityMode(str(cbQuality));
            VideoConfig.setQscale(((Number) spQscale.getValue()).intValue());
            VideoConfig.setCrf(((Number) spCrf.getValue()).intValue());
            VideoConfig.setBitrateKbps(((Number) spBitrate.getValue()).intValue());

            VideoConfig.setColorRange(clean(tfColorRange.getText()));
            VideoConfig.setColorspace(clean(tfColorSpace.getText()));
            VideoConfig.setColorTrc(clean(tfColorTrc.getText()));
            VideoConfig.setColorPrimaries(clean(tfColorPrim.getText()));
            VideoConfig.setVf(clean(tfVf.getText()));

            VideoConfig.setThreads(((Number) spThreads.getValue()).intValue());
            VideoConfig.setEnforceEvenDims(cbEvenDims.isSelected());
            VideoConfig.setContainerFallbacks(fbs);

            VideoConfig.setPreset(emptyToNull(tfPreset.getText()));
            VideoConfig.setTune(emptyToNull(tfTune.getText()));
            VideoConfig.setProfile(emptyToNull(tfProfile.getText()));
            VideoConfig.setLevel(emptyToNull(tfLevel.getText()));

            VideoConfig.getExtraVideoOptions().clear();
            VideoConfig.getExtraVideoOptions().putAll(extra);

            return true;
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Speichern fehlgeschlagen:\n" + ex.getMessage(),
                    "Fehler", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private void updateVlcUiEnabled() {
        boolean autod = cbVlcAutodetect != null && cbVlcAutodetect.isSelected();
        boolean vlcOn = cbVlcEnabled != null && cbVlcEnabled.isSelected();
        boolean pathEnabled = vlcOn && !autod;
        if (tfVlcBasePath != null) tfVlcBasePath.setEnabled(pathEnabled);
        if (btVlcBaseBrowse != null) btVlcBaseBrowse.setEnabled(pathEnabled);
        boolean logOn = cbVlcLogEnabled != null && cbVlcLogEnabled.isSelected();
        if (tfVlcLogPath != null) tfVlcLogPath.setEnabled(vlcOn && logOn);
        if (btVlcLogBrowse != null) btVlcLogBrowse.setEnabled(vlcOn && logOn);

        // Quali Felder schalten
        boolean isCrf = cbVlcQuality != null && "crf".equals(String.valueOf(cbVlcQuality.getSelectedItem()));
        if (spVlcCrf != null) spVlcCrf.setEnabled(isCrf);
        if (spVlcBitrate != null) spVlcBitrate.setEnabled(!isCrf);

        boolean regionEnabled = cbVlcFullscreen != null && !cbVlcFullscreen.isSelected();
        if (spVlcLeft != null) spVlcLeft.setEnabled(regionEnabled);
        if (spVlcTop  != null) spVlcTop.setEnabled(regionEnabled);
        if (spVlcWidth!= null) spVlcWidth.setEnabled(regionEnabled);
        if (spVlcHeight!= null) spVlcHeight.setEnabled(regionEnabled);
    }

    private void updateQualityCard() {
        // Nur Darstellung – die Card wird in buildUI() per ActionListener umgeschaltet.
    }

    // ---------- Helpers ----------

    private static void setInt(JSpinner sp, int v) { sp.setValue(v); }
    private static <T> T or(T v, T d) { return v != null ? v : d; }
    private static void sel(JComboBox<String> cb, String v, String d) {
        String val = (v==null||v.isEmpty()) ? d : v;
        cb.setSelectedItem(val);
    }
    private static String str(JComboBox<String> cb) {
        Object o = cb.getSelectedItem();
        return o==null? "" : o.toString();
    }
    private static String clean(String s) { return s==null ? "" : s.trim(); }
    private static String emptyToNull(String s) { s = clean(s); return s.isEmpty()? null : s; }

    private static List<String> parseCsv(String csv) {
        if (csv == null) return Collections.emptyList();
        String[] parts = csv.split(",");
        ArrayList<String> out = new ArrayList<>();
        for (String p : parts) {
            String t = p.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    private static String defaultVlcBasePath() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) return "C:/Program Files/VideoLAN/VLC";
        if (os.contains("mac")) return "/Applications/VLC.app/Contents/MacOS/lib";
        return "/usr/lib";
    }
    private static String defaultVlcLogPath() {
        return new File(System.getProperty("user.home"), ".wd4j/vlc.log").getAbsolutePath();
    }

    // Datei-/Ordnerauswahl für kleine Quadrat-Buttons
    private void chooseDirInto(JTextField target) {
        JFileChooser ch = new JFileChooser();
        ch.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        String cur = target.getText();
        if (cur != null && !cur.trim().isEmpty()) {
            File f = new File(cur.trim());
            if (f.exists()) ch.setCurrentDirectory(f.isDirectory()? f : f.getParentFile());
        }
        if (ch.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File dir = ch.getSelectedFile();
            if (dir != null) target.setText(dir.getAbsolutePath());
        }
    }

    private void chooseFileInto(JTextField target) {
        JFileChooser ch = new JFileChooser();
        String cur = target.getText();
        if (cur != null && !cur.trim().isEmpty()) {
            File f = new File(cur.trim());
            if (f.exists()) ch.setCurrentDirectory(f.isDirectory()? f : f.getParentFile());
        }
        if (ch.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = ch.getSelectedFile();
            if (file != null) target.setText(file.getAbsolutePath());
        }
    }
}
