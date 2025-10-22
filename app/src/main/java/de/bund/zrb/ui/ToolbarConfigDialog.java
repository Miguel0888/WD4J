package de.bund.zrb.ui;

import de.bund.zrb.ui.commandframework.MenuCommand;
import de.bund.zrb.ui.commandframework.ToolbarButtonConfig;
import de.bund.zrb.ui.commandframework.ToolbarConfig;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Dialog zur Konfiguration der Toolbar:
 * - Gruppiert nach Präfix (alles vor dem ersten '.')
 * - Größere Icon-Auswahl (editierbar, unterstützt Hex-Codepoints)
 * - Gruppenfarbe je Tab (+ Colorpicker)
 * - Button-Farbe mit Farbvorschau im Dropdown (+ Colorpicker)
 * - Positionskollisionen werden automatisch aufgelöst (stabil, global)
 * - "Sichtbarkeit…" zeigt/verbirgt Commands (wirkt sofort)
 */
public class ToolbarConfigDialog extends JDialog {

    private ToolbarConfig initialConfig;
    private final List<MenuCommand> allCommands;
    private final String[] iconSuggestions;

    private final JTabbedPane tabs = new JTabbedPane();
    private final Map<String, JPanel> groupPanels = new LinkedHashMap<>();
    private final Map<String, JTextField> groupColorFields = new LinkedHashMap<>();

    // pro Command: Controls
    private final Map<MenuCommand, JCheckBox> cbEnabled = new LinkedHashMap<>();
    private final Map<MenuCommand, JComboBox<String>> cbIcon = new LinkedHashMap<>();
    private final Map<MenuCommand, JComboBox<String>> cbColor = new LinkedHashMap<>();
    private final Map<MenuCommand, JCheckBox> cbRight = new LinkedHashMap<>();
    private final Map<MenuCommand, JSpinner> spOrder = new LinkedHashMap<>();

    // unten: Size/Font
    private JSpinner spButtonSize;
    private JSpinner spFontRatio;

    private ToolbarConfig result;

    public ToolbarConfigDialog(Window owner,
                               ToolbarConfig currentConfig,
                               List<MenuCommand> commands,
                               String[] iconSuggestions) {
        super(owner, "Toolbar konfigurieren", ModalityType.APPLICATION_MODAL);
        this.initialConfig = deepCopyOrInit(currentConfig);
        this.allCommands = new ArrayList<>(commands);
        this.iconSuggestions = iconSuggestions != null ? iconSuggestions : new String[0];

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(buildUI()); // baut mit Filter (hiddenCommandIds)
        pack();
        setLocationRelativeTo(owner);
        setMinimumSize(new Dimension(Math.max(820, getWidth()), Math.max(560, getHeight())));
    }

    public ToolbarConfig showDialog() {
        setVisible(true);
        return result;
    }

    // ---------------- UI ----------------

    private JComponent buildUI() {
        // ⚠️ WICHTIG: alte Controls verwerfen, damit der Zustand konsistent ist
        cbEnabled.clear();
        cbIcon.clear();
        cbColor.clear();
        cbRight.clear();
        spOrder.clear();

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(10, 12, 10, 12));

        // TOP-Leiste: links Sichtbarkeit/Defaults, rechts OK/Cancel
        JPanel top = new JPanel(new BorderLayout());
        JPanel leftTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton btVisibility = new JButton("Sichtbarkeit…");
        JButton btDefaults   = new JButton("Standard laden");
        leftTop.add(btVisibility);
        leftTop.add(btDefaults);
        JPanel rightTop = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton btOk     = new JButton("OK");
        JButton btCancel = new JButton("Abbrechen");
        rightTop.add(btOk);
        rightTop.add(btCancel);
        top.add(leftTop, BorderLayout.WEST);
        top.add(rightTop, BorderLayout.EAST);

        // Tabs pro Gruppe (nur nicht-versteckte Commands)
        tabs.removeAll();
        groupPanels.clear();
        groupColorFields.clear();

        Map<String, List<MenuCommand>> byGroup = groupCommands(filteredCommands());
        for (Map.Entry<String, List<MenuCommand>> ge : byGroup.entrySet()) {
            String group = ge.getKey();
            JPanel panel = buildGroupPanel(group, ge.getValue());
            JScrollPane scroll = new JScrollPane(panel);
            scroll.setBorder(BorderFactory.createEmptyBorder());
            tabs.addTab(group, scroll);
            groupPanels.put(group, panel);
        }

        // Größen-Panel unten
        JPanel sizePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        sizePanel.add(new JLabel("Buttongröße:"));
        spButtonSize = new JSpinner(new SpinnerNumberModel(
                Math.max(24, initialConfig.buttonSizePx), 24, 128, 4));
        sizePanel.add(spButtonSize);

        sizePanel.add(new JLabel("Schrift %:"));
        spFontRatio = new JSpinner(new SpinnerNumberModel(
                (double)Math.max(0.3f, Math.min(1.0f, initialConfig.fontSizeRatio)), 0.3, 1.0, 0.05));
        sizePanel.add(spFontRatio);

        // Actions (keine ContentPane-Ersetzung hier, damit OK/Cancel sicher funktionieren)
        btVisibility.addActionListener(e -> openVisibilityDialog());
        btDefaults.addActionListener(e -> {
            ToolbarConfig tmp = deepCopyOrInit(initialConfig);
            tmp.buttons = buildImportantDefaultButtons(); // reduzierte, wichtige Defaults
            // hidden neu ableiten: alles, was nicht sichtbar ist
            tmp.hiddenCommandIds.clear();
            Set<String> vis = new LinkedHashSet<>();
            for (ToolbarButtonConfig b : tmp.buttons) vis.add(b.id);
            for (String id : allCommandIds()) if (!vis.contains(id)) tmp.hiddenCommandIds.add(id);

            initialConfig = tmp;
            // Hauptdialog neu aufbauen:
            setContentPane(buildUI());
            revalidate(); repaint(); pack();
        });
        btOk.addActionListener(e -> onOk());
        btCancel.addActionListener(e -> onCancel());

        root.add(top, BorderLayout.NORTH);
        root.add(tabs, BorderLayout.CENTER);
        root.add(sizePanel, BorderLayout.SOUTH);
        return root;
    }

    private JPanel buildGroupPanel(String group, List<MenuCommand> cmds) {
        // Sortiere innerhalb der Gruppe nach aktueller Order (global) dann Label
        cmds.sort((a, b) -> {
            int oa = getOrderFor(a.getId());
            int ob = getOrderFor(b.getId());
            if (oa != ob) return Integer.compare(normalizeOrder(oa), normalizeOrder(ob));
            return Objects.toString(a.getLabel(), "")
                    .compareToIgnoreCase(Objects.toString(b.getLabel(), ""));
        });

        JPanel groupRoot = new JPanel(new BorderLayout(8, 8));

        // Kopfzeile: Gruppenfarbe (+ Preview + Picker)
        JPanel head = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        head.add(new JLabel("Gruppenfarbe (HEX, optional):"));
        String preset = initialConfig.groupColors.getOrDefault(group, "");
        JTextField tfGroupColor = new JTextField(preset, 10);
        head.add(tfGroupColor);

        JLabel lbPreview = makeColorPreviewLabel(tfGroupColor.getText());
        head.add(lbPreview);

        JButton btPick = makeColorPickerButton(() -> tfGroupColor.getText(), hex -> {
            tfGroupColor.setText(hex == null ? "" : hex);
        });
        head.add(btPick);

        head.add(new JLabel(" z.B. #FFD700"));
        groupColorFields.put(group, tfGroupColor);

        // Live-Preview für Gruppenfeld
        wireColorFieldPreview(tfGroupColor, lbPreview);

        // Liste der Commands
        JPanel list = new JPanel(new GridLayout(0, 1, 0, 4));

        for (MenuCommand cmd : cmds) {
            JPanel line = new JPanel(new BorderLayout(6, 0));

            JCheckBox enabled = new JCheckBox(cmd.getLabel(), isCommandActive(cmd.getId()));
            cbEnabled.put(cmd, enabled);

            // Icon-Auswahl (editierbar, groß gerendert)
            JComboBox<String> iconCombo = new JComboBox<>(iconSuggestions);
            iconCombo.setEditable(true);
            iconCombo.setSelectedItem(getIconFor(cmd.getId()));
            iconCombo.setPreferredSize(new Dimension(120, 38));
            iconCombo.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    c.setFont(c.getFont().deriveFont(20f));
                    if (c instanceof JLabel) ((JLabel)c).setHorizontalAlignment(SwingConstants.CENTER);
                    return c;
                }
            });
            Component ed = iconCombo.getEditor().getEditorComponent();
            ed.setFont(ed.getFont().deriveFont(20f));
            cbIcon.put(cmd, iconCombo);

            // Button-Farbe (editierbar) + Renderer mit Farbfeld + Picker
            JComboBox<String> colorCombo = new JComboBox<>(new String[]{
                    "", "#FF0000", "#00AA00", "#008000", "#FFA500", "#0000FF", "#FFFF00"
            });
            colorCombo.setEditable(true);
            String preHex = getBackgroundHexFor(cmd.getId());
            colorCombo.setSelectedItem(preHex == null ? "" : preHex);
            colorCombo.setPreferredSize(new Dimension(110, 28));
            colorCombo.setRenderer(new ColorCellRenderer());
            setEditorColorPreview(colorCombo); // live editor preview
            cbColor.put(cmd, colorCombo);

            JButton pickBtn = makeColorPickerButton(
                    () -> Objects.toString(colorCombo.getSelectedItem(), "").trim(),
                    hex -> {
                        if (hex == null) { colorCombo.setSelectedItem(""); }
                        else { colorCombo.setSelectedItem(hex); }
                    }
            );

            // Rechts-Seite
            boolean preRight = initialConfig.rightSideIds.contains(cmd.getId());
            JCheckBox rightChk = new JCheckBox("rechts", preRight);
            cbRight.put(cmd, rightChk);

            // Order
            int order = getOrderFor(cmd.getId());
            int initial = order > 0 ? order : suggestNextOrder();
            JSpinner sp = new JSpinner(new SpinnerNumberModel(initial, 1, 9999, 1));
            sp.setPreferredSize(new Dimension(72, 28));
            spOrder.put(cmd, sp);

            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
            right.add(new JLabel("Pos:"));
            right.add(sp);
            right.add(iconCombo);
            right.add(colorCombo);
            right.add(pickBtn);
            right.add(rightChk);

            line.add(enabled, BorderLayout.CENTER);
            line.add(right, BorderLayout.EAST);
            list.add(line);
        }

        groupRoot.add(head, BorderLayout.NORTH);
        groupRoot.add(list, BorderLayout.CENTER);
        return groupRoot;
    }

    // ---------------- Sichtbarkeits-Dialog ----------------

    private void openVisibilityDialog() {
        // Kopie der aktuellen Hidden-Menge
        LinkedHashSet<String> hidden = new LinkedHashSet<>();
        if (initialConfig.hiddenCommandIds != null) hidden.addAll(initialConfig.hiddenCommandIds);

        JDialog dlg = new JDialog(this, "Buttons ein-/ausblenden", Dialog.ModalityType.APPLICATION_MODAL);
        JPanel root = new JPanel(new BorderLayout(8,8));
        root.setBorder(new EmptyBorder(10,12,10,12));

        // Liste mit Checkboxen für ALLE Commands (alphabetisch nach Label)
        List<MenuCommand> sorted = new ArrayList<>(allCommands);
        sorted.sort(Comparator.comparing(mc -> Objects.toString(mc.getLabel(), Objects.toString(mc.getId(), ""))));

        JPanel list = new JPanel(new GridLayout(0,1,0,2));
        Map<MenuCommand, JCheckBox> checks = new LinkedHashMap<>();

        for (MenuCommand mc : sorted) {
            String id = Objects.toString(mc.getId(), "");
            boolean visible = !hidden.contains(id);
            JCheckBox cb = new JCheckBox(mc.getLabel() + "  (" + id + ")", visible);
            checks.put(mc, cb);
            list.add(cb);
        }

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8,0));
        JButton btAllOn  = new JButton("Alle anzeigen");
        JButton btAllOff = new JButton("Alle ausblenden");
        top.add(btAllOn); top.add(btAllOff);
        btAllOn.addActionListener(e -> checks.values().forEach(c -> c.setSelected(true)));
        btAllOff.addActionListener(e -> checks.values().forEach(c -> c.setSelected(false)));

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8,0));
        JButton btOk = new JButton("OK");
        JButton btCancel = new JButton("Abbrechen");
        bottom.add(btOk); bottom.add(btCancel);

        btCancel.addActionListener(e -> dlg.dispose());
        btOk.addActionListener(e -> {
            LinkedHashSet<String> newHidden = new LinkedHashSet<>();
            for (Map.Entry<MenuCommand, JCheckBox> en : checks.entrySet()) {
                String id = Objects.toString(en.getKey().getId(), "");
                if (!en.getValue().isSelected()) newHidden.add(id);
            }
            if (initialConfig.hiddenCommandIds == null) {
                initialConfig.hiddenCommandIds = new LinkedHashSet<>();
            }
            initialConfig.hiddenCommandIds.clear();
            initialConfig.hiddenCommandIds.addAll(newHidden);

            // Dialog schließen & Hauptdialog neu rendern
            dlg.dispose();

            // Neu rendern: Maps werden jetzt in buildUI() geleert
            setContentPane(buildUI());
            revalidate();
            repaint();
            pack();
        });

        root.add(top, BorderLayout.NORTH);
        root.add(scroll, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);
        dlg.setContentPane(root);
        dlg.pack();
        dlg.setMinimumSize(new Dimension(Math.max(560, dlg.getWidth()), Math.max(420, dlg.getHeight())));
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    // ---------------- Aktionen ----------------

    private void onOk() {
        ToolbarConfig cfg = deepCopyOrInit(initialConfig);
        cfg.buttons.clear();
        LinkedHashSet<String> newRight = new LinkedHashSet<>();

        // 1) Gruppenfarben übernehmen (normalisiert)
        for (Map.Entry<String, JTextField> e : groupColorFields.entrySet()) {
            String grp = e.getKey();
            String hex = normalizeHex(e.getValue().getText());
            if (hex == null) cfg.groupColors.remove(grp);
            else cfg.groupColors.put(grp, hex);
        }

        // 2) Alle selektierten Buttons einsammeln
        class Row {
            String id; String icon; String hex; int requestedOrder; boolean right;
            Row(String id, String icon, String hex, int ord, boolean r) {
                this.id=id; this.icon=icon; this.hex=hex; this.requestedOrder=ord; this.right=r;
            }
        }
        List<Row> rows = new ArrayList<>();

        for (MenuCommand cmd : cbEnabled.keySet()) {
            if (!cbEnabled.get(cmd).isSelected()) continue;

            String rawIcon = Objects.toString(cbIcon.get(cmd).getSelectedItem(), "").trim();
            String icon = normalizeIcon(rawIcon); // unterstützt U+XXXX / 0xXXXX / Fallback Text

            String hex = normalizeHex(Objects.toString(cbColor.get(cmd).getSelectedItem(), "").trim());
            int pos = ((Number) spOrder.get(cmd).getValue()).intValue();
            boolean right = cbRight.get(cmd).isSelected();

            rows.add(new Row(cmd.getId(), icon, hex, pos, right));
        }

        // 3) Positionskollisionen STABIL auflösen (global)
        rows.sort((a, b) -> {
            int c = Integer.compare(normalizeOrder(a.requestedOrder), normalizeOrder(b.requestedOrder));
            if (c != 0) return c;
            String la = labelOf(a.id);
            String lb = labelOf(b.id);
            return la.compareToIgnoreCase(lb);
        });

        TreeSet<Integer> used = new TreeSet<>();
        AtomicInteger maxAssigned = new AtomicInteger(0);
        for (Row r : rows) {
            int want = Math.max(1, r.requestedOrder);
            int assign = want;
            while (used.contains(assign)) assign++;
            used.add(assign);
            maxAssigned.set(Math.max(maxAssigned.get(), assign));

            ToolbarButtonConfig tbc = new ToolbarButtonConfig(r.id, r.icon);
            tbc.order = assign;
            tbc.backgroundHex = r.hex; // kann null sein (=> Gruppenfarbe gilt)
            cfg.buttons.add(tbc);
            if (r.right) newRight.add(r.id);
        }

        // 4) Größen / Seiten übernehmen
        cfg.buttonSizePx = ((Number) spButtonSize.getValue()).intValue();
        cfg.fontSizeRatio = ((Number) spFontRatio.getValue()).floatValue();
        cfg.rightSideIds = newRight;

        // 5) Hidden aus Checkbox-Dialog (bereits in initialConfig) übernehmen
        cfg.hiddenCommandIds = new LinkedHashSet<>(initialConfig.hiddenCommandIds);

        // 6) Ergebnis zurückgeben
        this.result = cfg;
        dispose();
    }

    private void onCancel() {
        this.result = null;
        dispose();
    }

    // ---------------- Helpers / Modell ----------------

    private ToolbarConfig deepCopyOrInit(ToolbarConfig in) {
        ToolbarConfig cfg = new ToolbarConfig();
        if (in == null) {
            cfg.buttonSizePx = 48;
            cfg.fontSizeRatio = 0.75f;
            cfg.buttons = new ArrayList<>();
            cfg.rightSideIds = new LinkedHashSet<>();
            cfg.groupColors = new LinkedHashMap<>();
            cfg.hiddenCommandIds = new LinkedHashSet<>();
            cfg.buttons.addAll(buildImportantDefaultButtons());
            // Hidden initial: alles übrige
            Set<String> vis = new LinkedHashSet<>();
            for (ToolbarButtonConfig b : cfg.buttons) vis.add(b.id);
            for (String id : allCommandIds()) if (!vis.contains(id)) cfg.hiddenCommandIds.add(id);
            return cfg;
        }
        cfg.buttonSizePx = (in.buttonSizePx > 0) ? in.buttonSizePx : 48;
        cfg.fontSizeRatio = (in.fontSizeRatio > 0f) ? in.fontSizeRatio : 0.75f;
        cfg.buttons = new ArrayList<>();
        if (in.buttons != null) {
            for (ToolbarButtonConfig b : in.buttons) {
                ToolbarButtonConfig nb = new ToolbarButtonConfig(b.id, b.icon);
                nb.order = b.order;
                nb.backgroundHex = b.backgroundHex;
                cfg.buttons.add(nb);
            }
        }
        cfg.rightSideIds = new LinkedHashSet<>();
        if (in.rightSideIds != null) cfg.rightSideIds.addAll(in.rightSideIds);
        cfg.groupColors = new LinkedHashMap<>();
        if (in.groupColors != null) cfg.groupColors.putAll(in.groupColors);
        cfg.hiddenCommandIds = new LinkedHashSet<>();
        if (in.hiddenCommandIds != null) cfg.hiddenCommandIds.addAll(in.hiddenCommandIds);
        return cfg;
    }

    private Map<String, List<MenuCommand>> groupCommands(List<MenuCommand> cmds) {
        Map<String, List<MenuCommand>> byGroup = new LinkedHashMap<>();
        for (MenuCommand cmd : cmds) {
            String id = Objects.toString(cmd.getId(), "");
            String grp = "(ohne)";
            int dot = id.indexOf('.');
            if (dot > 0) grp = id.substring(0, dot);
            byGroup.computeIfAbsent(grp, k -> new ArrayList<>()).add(cmd);
        }
        return byGroup;
    }

    private List<MenuCommand> filteredCommands() {
        // nur nicht-versteckte
        LinkedHashSet<String> hidden = initialConfig.hiddenCommandIds == null
                ? new LinkedHashSet<>() : initialConfig.hiddenCommandIds;
        List<MenuCommand> list = new ArrayList<>();
        for (MenuCommand mc : allCommands) {
            String id = Objects.toString(mc.getId(), "");
            if (!hidden.contains(id)) list.add(mc);
        }
        return list;
    }

    private int suggestNextOrder() {
        int max = 0;
        for (ToolbarButtonConfig b : initialConfig.buttons) {
            if (b.order != null && b.order > max) max = b.order;
        }
        return Math.max(1, max + 1);
    }

    private int getOrderFor(String id) {
        for (ToolbarButtonConfig b : initialConfig.buttons) {
            if (id.equals(b.id)) return (b.order == null) ? 0 : b.order;
        }
        return 0;
    }

    private String getIconFor(String id) {
        for (ToolbarButtonConfig b : initialConfig.buttons) {
            if (id.equals(b.id)) return b.icon != null ? b.icon : "🔘";
        }
        return "🔘";
    }

    private String getBackgroundHexFor(String id) {
        for (ToolbarButtonConfig b : initialConfig.buttons) {
            if (id.equals(b.id)) return b.backgroundHex;
        }
        return null;
    }

    private int normalizeOrder(int ord) {
        return (ord <= 0) ? Integer.MAX_VALUE : ord;
    }

    private String labelOf(String id) {
        for (MenuCommand mc : allCommands) {
            if (Objects.equals(mc.getId(), id)) return Objects.toString(mc.getLabel(), id);
        }
        return id;
    }

    private String normalizeIcon(String raw) {
        if (raw == null) return "●";
        String s = raw.trim();
        if (s.isEmpty()) return "●";
        String up = s.toUpperCase(Locale.ROOT);
        if (up.matches("^([U]\\+|0X)?[0-9A-F]{4,6}$")) {
            String hex = up.replace("U+", "").replace("0X", "");
            try {
                int cp = Integer.parseInt(hex, 16);
                return new String(Character.toChars(cp));
            } catch (Exception ignore) { /* rohen Text lassen */ }
        }
        return s;
    }

    private String normalizeHex(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty()) return null;
        if (!s.startsWith("#")) s = "#" + s;
        if (!s.matches("^#([0-9A-Fa-f]{3}|[0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$")) return null;
        return s.toUpperCase(Locale.ROOT);
    }

    // --- "Wichtige" Defaults (sichtbare Buttons) -----------------------------

    private List<ToolbarButtonConfig> buildImportantDefaultButtons() {
        List<ToolbarButtonConfig> visible = new ArrayList<>();

        String idPlay        = findIdContaining("testsuite.play", "playtestsuite", "playtest", "play");
        String idStopPlay    = findIdContaining("stopplayback", "stop.playback", "stopplay");
        String idStartRec    = findIdContaining("startrecord", "record.start");
        String idStopRec     = findIdContaining("stoprecord", "record.stop");
        String idLogin       = findIdContaining("loginuser", "login");
        String idHome        = findIdContaining("navigation.home", "home");
        String idOtp         = findIdContaining("showotpdialog", "otp");
        String idCloseTab    = findIdContaining("closetab", "close.tab");
        String idReload      = findIdContaining("reloadtab", "reload", "refresh");
        String idUserReg     = findIdContaining("userregistry", "user.registry", "credentials", "zugangsdaten");
        String idVideoToggle = findIdContaining("video.toggle", "recording.toggle", "video.record");

        class Def { String icon; String hex; Def(String i, String h){icon=i;hex=h;} }
        Map<String, Def> m = new LinkedHashMap<>();
        m.put(idPlay,     new Def("▶", "#00AA00"));
        m.put(idStopPlay, new Def("■", "#00AA00"));
        m.put(idStartRec, new Def("⦿", "#FF0000"));
        m.put(idStopRec,  new Def("■", "#FF0000"));
        m.put(idLogin,    new Def(new String(Character.toChars(0x1F511)), "#FFD700")); // 🔑
        m.put(idHome,     new Def(new String(Character.toChars(0x1F3E0)), "#FFD700")); // 🏠
        m.put(idOtp,      new Def("🔢", "#FFD700"));
        m.put(idCloseTab, new Def("✖", null));
        m.put(idReload,   new Def("↻", null));
        m.put(idUserReg,  new Def(new String(Character.toChars(0x1F4C7)), null)); // 📇
        m.put(idVideoToggle, new Def(new String(Character.toChars(0x1F3AC)), null)); // 🎬

        String[] order = new String[]{
                idPlay, idStopPlay, idStartRec, idStopRec, idVideoToggle,
                idLogin, idHome, idOtp, idCloseTab, idReload, idUserReg
        };

        int pos = 1;
        for (String id : order) {
            if (id == null) continue;
            Def d = m.get(id);
            ToolbarButtonConfig tbc = new ToolbarButtonConfig(id, d.icon);
            tbc.order = pos++;
            tbc.backgroundHex = d.hex;
            visible.add(tbc);
        }

        return visible;
    }

    private String findIdContaining(String... tokens) {
        for (String t : tokens) {
            String needle = t.toLowerCase(Locale.ROOT);
            for (MenuCommand mc : allCommands) {
                String id = Objects.toString(mc.getId(), "");
                if (id.toLowerCase(Locale.ROOT).contains(needle)) return id;
            }
        }
        return null;
    }

    private Set<String> allCommandIds() {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (MenuCommand mc : allCommands) {
            if (mc != null && mc.getId() != null) ids.add(mc.getId());
        }
        return ids;
    }

    // ---- Defaults/Fallbacks (wie gehabt) ------------------------------------




    private boolean isCommandActive(String id) {
        return initialConfig.buttons.stream().anyMatch(b -> b.id.equals(id));
    }

    // ---------------- Farb-UI Helpers (unverändert) --------------------------

    /** Renderer für Farblisten: zeigt ein Farbfeld + Hex ("" → (auto)). */
    private static final class ColorCellRenderer extends DefaultListCellRenderer {
        private static final Icon EMPTY_ICON = new ColorIcon(null, 14, 14);
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            String s = Objects.toString(value, "").trim();
            String hex = s.isEmpty() ? null : normalizeHexStatic(s);
            Color c = toColor(hex);
            lbl.setText(hex == null ? "(auto)" : hex);
            lbl.setIcon(c == null ? EMPTY_ICON : new ColorIcon(c, 14, 14));
            lbl.setHorizontalTextPosition(SwingConstants.RIGHT);
            lbl.setIconTextGap(8);
            lbl.setToolTipText(hex == null ? "leer = Gruppenfarbe (falls gesetzt), sonst Standard"
                    : hex);
            return lbl;
        }
    }

    private JButton makeColorPickerButton(Supplier<String> currentHexSupplier,
                                          Consumer<String> hexConsumer) {
        JButton btn = new JButton("■");
        btn.setMargin(new Insets(0,0,0,0));
        Dimension d = new Dimension(24, 24);
        btn.setPreferredSize(d);
        btn.setMinimumSize(d);
        btn.setMaximumSize(d);
        btn.setFocusable(false);
        btn.setToolTipText("Farbe wählen…");
        btn.addActionListener(e -> {
            String hex = normalizeHex(currentHexSupplier.get());
            Color base = toColor(hex);
            Color chosen = JColorChooser.showDialog(this, "Farbe wählen", base == null ? Color.WHITE : base);
            if (chosen != null) {
                String out = toHex(chosen);
                hexConsumer.accept(out);
            }
        });
        return btn;
    }

    private JLabel makeColorPreviewLabel(String hex) {
        JLabel l = new JLabel("  ");
        l.setOpaque(true);
        l.setBorder(BorderFactory.createLineBorder(new Color(0,0,0,80)));
        l.setPreferredSize(new Dimension(28, 18));
        applyPreviewColor(l, hex);
        return l;
    }

    private void wireColorFieldPreview(JTextField tf, JLabel preview) {
        tf.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { changed(); }
            public void removeUpdate(DocumentEvent e) { changed(); }
            public void changedUpdate(DocumentEvent e) { changed(); }
            private void changed() { applyPreviewColor(preview, tf.getText()); }
        });
    }

    private void setEditorColorPreview(JComboBox<String> combo) {
        Component editor = combo.getEditor().getEditorComponent();
        if (!(editor instanceof JTextField)) return;
        JTextField tf = (JTextField) editor;
        tf.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { update(); }
            public void removeUpdate(DocumentEvent e) { update(); }
            public void changedUpdate(DocumentEvent e) { update(); }
            private void update() {
                String hex = normalizeHex(tf.getText());
                Color c = toColor(hex);
                if (hex == null || c == null) {
                    tf.setBackground(UIManager.getColor("TextField.background"));
                    tf.setForeground(UIManager.getColor("TextField.foreground"));
                    return;
                }
                tf.setBackground(c);
                tf.setForeground(contrastColor(c));
            }
        });
        SwingUtilities.invokeLater(() -> {
            String hex = normalizeHex(tf.getText());
            Color c = toColor(hex);
            if (hex != null && c != null) {
                tf.setBackground(c);
                tf.setForeground(contrastColor(c));
            }
        });
    }

    private static void applyPreviewColor(JLabel l, String rawHex) {
        String hex = normalizeHexStatic(rawHex);
        Color c = toColor(hex);
        if (c == null) {
            l.setBackground(UIManager.getColor("Panel.background"));
            l.setToolTipText("leer = Gruppenfarbe deaktiviert");
        } else {
            l.setBackground(c);
            l.setToolTipText(hex);
        }
    }

    // ---------------- Farb-Utils ----------------

    private static String normalizeHexStatic(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty()) return null;
        if (!s.startsWith("#")) s = "#" + s;
        if (!s.matches("^#([0-9A-Fa-f]{3}|[0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$")) return null;
        return s.toUpperCase(Locale.ROOT);
    }

    private static Color toColor(String hex) {
        if (hex == null) return null;
        try {
            String h = hex.substring(1);
            if (h.length() == 3) {
                int r = Integer.parseInt(h.substring(0,1)+h.substring(0,1), 16);
                int g = Integer.parseInt(h.substring(1,2)+h.substring(1,2), 16);
                int b = Integer.parseInt(h.substring(2,3)+h.substring(2,3), 16);
                return new Color(r,g,b);
            } else if (h.length() == 6) {
                int rgb = Integer.parseInt(h, 16);
                return new Color(rgb);
            } else if (h.length() == 8) {
                long v = Long.parseLong(h, 16);
                int a = (int)((v >> 24) & 0xFF);
                int r = (int)((v >> 16) & 0xFF);
                int g = (int)((v >> 8)  & 0xFF);
                int b = (int)(v & 0xFF);
                return new Color(r,g,b,a);
            }
        } catch (Exception ignore) {}
        return null;
    }

    private static String toHex(Color c) {
        if (c == null) return null;
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }

    private static Color contrastColor(Color bg) {
        double luma = 0.2126*bg.getRed() + 0.7152*bg.getGreen() + 0.0722*bg.getBlue();
        return (luma < 140) ? Color.WHITE : Color.BLACK;
    }

    private static final class ColorIcon implements Icon {
        private final Color color;
        private final int w, h;
        ColorIcon(Color c, int w, int h) { this.color = c; this.w=w; this.h=h; }
        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color == null ? new Color(0,0,0,0) : color);
                g2.fillRoundRect(x, y, w, h, 3, 3);
                g2.setColor(new Color(0,0,0,90));
                g2.drawRoundRect(x, y, w, h, 3, 3);
            } finally {
                g2.dispose();
            }
        }
        @Override public int getIconWidth() { return w; }
        @Override public int getIconHeight() { return h; }
    }
}
