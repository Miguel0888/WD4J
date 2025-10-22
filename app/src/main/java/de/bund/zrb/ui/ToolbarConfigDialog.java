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
 * - Hidden-Set wird automatisch aus allen Commands abgeleitet
 * - "Standard laden" zeigt nur die wichtigsten Buttons (siehe Mapping unten)
 */
public class ToolbarConfigDialog extends JDialog {

    private final ToolbarConfig initialConfig;
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
        setContentPane(buildUI());
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
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(10, 12, 10, 12));

        Map<String, List<MenuCommand>> byGroup = groupCommands(allCommands);
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

        // Buttons
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton btDefaults = new JButton("Standard laden");
        JButton btOk = new JButton("OK");
        JButton btCancel = new JButton("Abbrechen");

        btDefaults.addActionListener(e -> resetToDefaults());
        btOk.addActionListener(e -> onOk());
        btCancel.addActionListener(e -> onCancel());

        footer.add(btDefaults);
        footer.add(btOk);
        footer.add(btCancel);

        root.add(tabs, BorderLayout.CENTER);
        root.add(sizePanel, BorderLayout.SOUTH);
        root.add(footer, BorderLayout.NORTH);

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

    // ---------------- Aktionen ----------------

    private void resetToDefaults() {
        // Buttons auf wichtige Defaults setzen, Größen/RightSide/GroupColors beibehalten
        ToolbarConfig tmp = deepCopyOrInit(initialConfig);
        tmp.buttons = buildImportantDefaultButtons();
        // Ergebnis anwenden und UI neu aufbauen
        applyConfig(tmp);
    }

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

        // 5) Hidden aus allen Commands ableiten
        LinkedHashSet<String> hidden = new LinkedHashSet<>();
        Set<String> visibleIds = new LinkedHashSet<>();
        for (ToolbarButtonConfig b : cfg.buttons) if (b != null && b.id != null) visibleIds.add(b.id);
        for (String id : allCommandIds()) if (!visibleIds.contains(id)) hidden.add(id);
        if (cfg.hiddenCommandIds == null) cfg.hiddenCommandIds = new LinkedHashSet<>();
        cfg.hiddenCommandIds.clear();
        cfg.hiddenCommandIds.addAll(hidden);

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
            // Hidden initial befüllen
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

    private void applyConfig(ToolbarConfig cfg) {
        // Dialog neu aufbauen mit cfg
        getContentPane().removeAll();
        this.result = null; // noch nicht bestätigt
        ToolbarConfigDialog fresh = new ToolbarConfigDialog(getOwner(), cfg, allCommands, iconSuggestions);
        // „Ersetze“ den Inhalt
        setContentPane(fresh.getContentPane());
        this.spButtonSize = fresh.spButtonSize;
        this.spFontRatio = fresh.spFontRatio;
        this.tabs.removeAll();
        for (int i = 0; i < fresh.tabs.getTabCount(); i++) {
            this.tabs.addTab(fresh.tabs.getTitleAt(i), fresh.tabs.getComponentAt(i));
        }
        this.cbEnabled.clear(); this.cbEnabled.putAll(fresh.cbEnabled);
        this.cbIcon.clear(); this.cbIcon.putAll(fresh.cbIcon);
        this.cbColor.clear(); this.cbColor.putAll(fresh.cbColor);
        this.cbRight.clear(); this.cbRight.putAll(fresh.cbRight);
        this.spOrder.clear(); this.spOrder.putAll(fresh.spOrder);
        this.groupPanels.clear(); this.groupPanels.putAll(fresh.groupPanels);
        this.groupColorFields.clear(); this.groupColorFields.putAll(fresh.groupColorFields);

        revalidate();
        repaint();
        pack();
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
        // #RGB / #RRGGBB / #RRGGBBAA akzeptieren (Swing-JColorChooser zeigt ohnehin ohne Alpha)
        if (!s.matches("^#([0-9A-Fa-f]{3}|[0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$")) return null;
        return s.toUpperCase(Locale.ROOT);
    }

    // --- "Wichtige" Defaults (sichtbare Buttons) -----------------------------

    private List<ToolbarButtonConfig> buildImportantDefaultButtons() {
        List<ToolbarButtonConfig> visible = new ArrayList<>();

        // Fuzzy-Suche nach IDs im Registry-Bestand
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

        // Mapping: Icons + Farben
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

        // Reihenfolge
        String[] order = new String[]{
                idPlay, idStopPlay, idStartRec, idStopRec,
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

    // --- (Fallback) Defaults für Buttons – ungenutzt, aber belassen -----------
    // Wird nicht mehr von resetToDefaults verwendet, bleibt als Reserve erhalten.
    @SuppressWarnings("unused")
    private List<ToolbarButtonConfig> buildDefaultButtonsForAllCommands() {
        List<ToolbarButtonConfig> list = new ArrayList<>();
        List<MenuCommand> cmds = new ArrayList<>(allCommands);
        cmds.sort(Comparator.comparing(m -> Objects.toString(m.getLabel(), "")));
        int pos = 1;
        for (MenuCommand cmd : cmds) {
            ToolbarButtonConfig tbc = new ToolbarButtonConfig(cmd.getId(), defaultIconFor(cmd.getId()));
            tbc.order = pos++;
            String bg = defaultBackgroundHexFor(cmd.getId());
            if (bg != null) tbc.backgroundHex = bg;
            list.add(tbc);
        }
        return list;
    }

    // dieselben Regeln wie in ActionToolbar (kompakt)
    private String defaultIconFor(String idRaw) {
        String id = idRaw == null ? "" : idRaw.toLowerCase(Locale.ROOT);
        if (id.contains("record.play"))      return "▶";
        if (id.contains("record.stop"))      return "■";
        if (id.contains("record.toggle"))    return "⦿";
        if (id.contains("testsuite.play"))   return "▶";
        if (id.contains("testsuite.stop"))   return "■";
        if (id.contains("browser.launch") || id.contains("launch")) return new String(Character.toChars(0x1F310)); // 🌐
        if (id.contains("terminate"))                                return "■";
        if (id.contains("newtab"))                                   return "＋";
        if (id.contains("closetab") || id.contains("close"))         return "✖";
        if (id.contains("reload") || id.contains("refresh"))         return "↻";
        if (id.contains("back"))                                     return "←";
        if (id.contains("forward"))                                  return "→";
        if (id.contains("home"))                                     return new String(Character.toChars(0x1F3E0)); // 🏠
        if (id.contains("screenshot") || id.contains("capture"))     return new String(Character.toChars(0x1F4F7)); // 📷
        if (id.contains("selectors"))                                return new String(Character.toChars(0x1F50D)); // 🔍
        if (id.contains("domevents"))                                return new String(Character.toChars(0x1F4DC)); // 📜
        if (id.contains("userselection") || id.contains("userregistry")) return new String(Character.toChars(0x1F4C7)); // 📇
        if (id.contains("login") || id.contains("otp"))                   return new String(Character.toChars(0x1F511)); // 🔑
        if (id.contains("view.toggleleft"))                           return "⟨";
        if (id.contains("view.toggleright"))                          return "⟩";
        if (id.contains("settings") || id.contains("configure"))      return "⚙";
        if (id.contains("shortcut"))                                  return "⌘";
        return "●";
    }

    private String defaultBackgroundHexFor(String idRaw) {
        String id = idRaw == null ? "" : idRaw.toLowerCase(Locale.ROOT);
        if (id.contains("record")) return "#FF0000";
        if (id.contains("testsuite.play") || id.contains("play")) return "#00AA00";
        return null;
    }

    private boolean isCommandActive(String id) {
        return initialConfig.buttons.stream().anyMatch(b -> b.id.equals(id));
    }

    // ---------------- Farb-UI Helpers ----------------

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

    /** Kleiner quadratischer Button, der JColorChooser öffnet. */
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

    /** Farbvorschau-Label für das Gruppenfeld. */
    private JLabel makeColorPreviewLabel(String hex) {
        JLabel l = new JLabel("  ");
        l.setOpaque(true);
        l.setBorder(BorderFactory.createLineBorder(new Color(0,0,0,80)));
        l.setPreferredSize(new Dimension(28, 18));
        applyPreviewColor(l, hex);
        return l;
    }

    /** Live-Update der Gruppen-Preview. */
    private void wireColorFieldPreview(JTextField tf, JLabel preview) {
        tf.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { changed(); }
            public void removeUpdate(DocumentEvent e) { changed(); }
            public void changedUpdate(DocumentEvent e) { changed(); }
            private void changed() { applyPreviewColor(preview, tf.getText()); }
        });
    }

    /** Live-Preview für ComboBox-Editor (Hintergrund einfärben + Kontrastschrift). */
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
        // Initial
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
                long v = Long.parseLong(h, 16); // ARGB?
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
        // einfache Luma-Heuristik
        double luma = 0.2126*bg.getRed() + 0.7152*bg.getGreen() + 0.0722*bg.getBlue();
        return (luma < 140) ? Color.WHITE : Color.BLACK;
    }

    /** Kleines Farbfeld-Icon. */
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
