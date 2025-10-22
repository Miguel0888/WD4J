package de.bund.zrb.ui.video;

import de.bund.zrb.config.VideoConfig;
import de.bund.zrb.service.SettingsService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;

public class VideoSettingsDialog extends JDialog {

    private JComboBox<String> cbContainer, cbCodec, cbPixFmt, cbQuality;
    private JCheckBox cbInterleaved, cbEvenDims;
    private JSpinner spThreads, spQscale, spCrf, spBitrate;
    private JTextField tfColorRange, tfColorSpace, tfColorTrc, tfColorPrim, tfPreset, tfTune, tfProfile, tfLevel;
    private JTextField tfVf, tfFallbacksCsv;
    private JTable tblExtra;
    private DefaultTableModel extraModel;

    public VideoSettingsDialog(Window owner) {
        super(owner, "Video-Details", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(buildUI());
        pack();
        setLocationRelativeTo(owner);
        loadFromSettings();
        updateQualityCard();
    }

    private JPanel buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(new EmptyBorder(10,12,10,12));

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

        // zusammensetzen
        form.add(pEnc);      form.add(Box.createVerticalStrut(8));
        form.add(pQ);        form.add(Box.createVerticalStrut(8));
        form.add(pColor);    form.add(Box.createVerticalStrut(8));
        form.add(pSys);      form.add(Box.createVerticalStrut(8));
        form.add(pX);        form.add(Box.createVerticalStrut(8));
        form.add(pExtra);

        // Footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton btReset = new JButton("Defaults");
        JButton btOk    = new JButton("OK");
        JButton btCancel= new JButton("Abbrechen");
        btReset.addActionListener(e -> loadDefaults());
        btOk.addActionListener(e -> { if (save()) dispose(); });
        btCancel.addActionListener(e -> dispose());
        footer.add(btReset); footer.add(btOk); footer.add(btCancel);

        root.add(form, BorderLayout.CENTER);
        root.add(Box.createVerticalStrut(8), BorderLayout.NORTH);
        root.add(footer, BorderLayout.SOUTH);
        return root;
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
        updateQualityCard();
    }

    private void loadDefaults() {
        // Lege die in VideoConfig definierten Defaults nahe — hier simpel hardcodiert auf gängige Defaults:
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
        updateQualityCard();
    }

    private boolean save() {
        try {
            SettingsService s = SettingsService.getInstance();

            // Settings persistieren
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

            // Extra-Optionen als Map<String,String>
            Map<String,String> extra = new LinkedHashMap<>();
            for (int i=0; i<extraModel.getRowCount(); i++) {
                String k = clean(String.valueOf(extraModel.getValueAt(i,0)));
                String v = String.valueOf(extraModel.getValueAt(i,1));
                if (k != null && !k.isEmpty() && v != null) extra.put(k, v);
            }
            s.set("video.ffopts", extra);

            // Live in VideoConfig übernehmen
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

    private void updateQualityCard() {
        // Nur Darstellung – die Card wird in buildUI() per ActionListener umgeschaltet.
        // Hier kann man ggf. Validierungs-/Enable-Logik ergänzen.
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
}
