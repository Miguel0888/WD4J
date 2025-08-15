package de.bund.zrb.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.bund.zrb.ui.commandframework.CommandRegistryImpl;
import de.bund.zrb.ui.commandframework.MenuCommand;
import de.bund.zrb.ui.commandframework.ToolbarButtonConfig;
import de.bund.zrb.ui.commandframework.ToolbarConfig;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;

public class ActionToolbar extends JToolBar {

    private ToolbarConfig config;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public ActionToolbar() {
        setFloatable(false);

        loadToolbarSettings();
        rebuildButtons();
    }

    // ÄNDERT: Hintergrundfarbe anwenden, falls gesetzt
    // ÄNDERT: Buttons je nach ID-Set links oder rechts platzieren (Farbhintergründe bleiben erhalten)
    // ÄNDERT: Buttons sortieren nach 'order' (1 = ganz links). Fallback: Label.
    // ÄNDERT: Buttons nach 'order' sortieren und Background-Farbe anwenden
    private void rebuildButtons() {
        removeAll();

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 2));

        if (config != null && config.buttons != null) {
            // Split by side
            java.util.List<ToolbarButtonConfig> left = new ArrayList<ToolbarButtonConfig>();
            java.util.List<ToolbarButtonConfig> right = new ArrayList<ToolbarButtonConfig>();

            for (ToolbarButtonConfig b : config.buttons) {
                if (isRightAligned(b.id)) {
                    right.add(b);
                } else {
                    left.add(b);
                }
            }

            // Sort by order (1..n). Unknown/invalid orders go to the end.
            Collections.sort(left, buttonOrderComparator());
            Collections.sort(right, buttonOrderComparator());

            // Build left
            for (final ToolbarButtonConfig btnCfg : left) {
                CommandRegistryImpl.getInstance().getById(btnCfg.id).ifPresent(cmd -> {
                    JButton btn = new JButton(btnCfg.icon);
                    btn.setMargin(new Insets(0, 0, 0, 0));
                    btn.setToolTipText(cmd.getLabel());
                    btn.setPreferredSize(new Dimension(config.buttonSizePx, config.buttonSizePx));

                    int fontSize = (int) (config.buttonSizePx * config.fontSizeRatio);
                    btn.setFont(btn.getFont().deriveFont((float) fontSize));
                    btn.setFocusPainted(false);

                    // Apply background color if configured
                    if (btnCfg.backgroundHex != null && btnCfg.backgroundHex.trim().length() > 0) {
                        try {
                            String hex = btnCfg.backgroundHex.trim();
                            if (!hex.startsWith("#")) hex = "#" + hex;
                            btn.setOpaque(true);
                            btn.setContentAreaFilled(true);
                            btn.setBackground(Color.decode(hex));
                        } catch (NumberFormatException ignore) {
                            // Ignore invalid hex
                        }
                    }

                    btn.addActionListener(e -> cmd.perform());
                    leftPanel.add(btn);
                });
            }

            // Build right (before gear)
            for (final ToolbarButtonConfig btnCfg : right) {
                CommandRegistryImpl.getInstance().getById(btnCfg.id).ifPresent(cmd -> {
                    JButton btn = new JButton(btnCfg.icon);
                    btn.setMargin(new Insets(0, 0, 0, 0));
                    btn.setToolTipText(cmd.getLabel());
                    btn.setPreferredSize(new Dimension(config.buttonSizePx, config.buttonSizePx));

                    int fontSize = (int) (config.buttonSizePx * config.fontSizeRatio);
                    btn.setFont(btn.getFont().deriveFont((float) fontSize));
                    btn.setFocusPainted(false);

                    if (btnCfg.backgroundHex != null && btnCfg.backgroundHex.trim().length() > 0) {
                        try {
                            String hex = btnCfg.backgroundHex.trim();
                            if (!hex.startsWith("#")) hex = "#" + hex;
                            btn.setOpaque(true);
                            btn.setContentAreaFilled(true);
                            btn.setBackground(Color.decode(hex));
                        } catch (NumberFormatException ignore) {
                            // Ignore invalid hex
                        }
                    }

                    btn.addActionListener(e -> cmd.perform());
                    rightPanel.add(btn);
                });
            }
        }

        // Gear button stays at far right
        JButton configBtn = new JButton("⚙");
        configBtn.setMargin(new Insets(0, 0, 0, 0));
        configBtn.setToolTipText("Toolbar anpassen");
        configBtn.setPreferredSize(new Dimension(config.buttonSizePx, config.buttonSizePx));
        int fontSize = (int) (config.buttonSizePx * config.fontSizeRatio);
        configBtn.setFont(configBtn.getFont().deriveFont((float) fontSize));
        configBtn.setFocusPainted(false);
        configBtn.addActionListener(e -> openConfigDialog());
        rightPanel.add(configBtn);

        setLayout(new BorderLayout());
        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.EAST);

        revalidate();
        repaint();
    }

    // ÄNDERT: zweite Spalte für Farbe hinzufügen und speichern; "Standard laden" bleibt
    // ÄNDERT: Menü um "Position" (ganzzahlige Reihenfolge) erweitern, Duplikate verhindern
    // ÄNDERT: Menü mit Pos(ition)-Spinner (Schritt 1, sinnvolle Defaults), Farbspalte, Duplikat-Check
    private void openConfigDialog() {
        if (config.rightSideIds == null) {
            config.rightSideIds = new LinkedHashSet<String>();
        }

        // Track used orders to compute next free suggestion
        final LinkedHashSet<Integer> usedOrders = new LinkedHashSet<Integer>();
        if (config.buttons != null) {
            for (ToolbarButtonConfig b : config.buttons) {
                if (b != null && b.order != null && b.order.intValue() > 0) {
                    usedOrders.add(Integer.valueOf(b.order.intValue()));
                }
            }
        }
        // Helper to compute next free order
        int nextFreeOrder = 1;
        while (usedOrders.contains(Integer.valueOf(nextFreeOrder))) nextFreeOrder++;

        List<MenuCommand> all = new ArrayList<MenuCommand>(CommandRegistryImpl.getInstance().getAll());
        Map<MenuCommand, JCheckBox> checkboxes = new LinkedHashMap<MenuCommand, JCheckBox>();
        Map<MenuCommand, JComboBox<String>> iconSelectors = new LinkedHashMap<MenuCommand, JComboBox<String>>();
        Map<MenuCommand, JComboBox<String>> colorSelectors = new LinkedHashMap<MenuCommand, JComboBox<String>>();
        Map<MenuCommand, JCheckBox> rightSideChecks = new LinkedHashMap<MenuCommand, JCheckBox>();
        Map<MenuCommand, JSpinner> orderSpinners = new LinkedHashMap<MenuCommand, JSpinner>();

        JPanel commandPanel = new JPanel(new GridLayout(0, 1));
        for (MenuCommand cmd : all) {
            JPanel line = new JPanel(new BorderLayout(4, 0));
            JCheckBox box = new JCheckBox(cmd.getLabel(), isCommandActive(cmd.getId()));

            // Icon selector
            JComboBox<String> iconCombo = new JComboBox<String>(getSimpleIconSuggestions());
            iconCombo.setEditable(true);
            iconCombo.setSelectedItem(getIconFor(cmd.getId()));
            iconCombo.setPreferredSize(new Dimension(56, 24));
            iconSelectors.put(cmd, iconCombo);

            // Color selector (editable HEX)
            JComboBox<String> colorCombo = new JComboBox<String>(new String[] {
                    "", "#FF0000", "#00AA00", "#008000", "#FFA500", "#0000FF", "#FFFF00"
            });
            colorCombo.setEditable(true);
            String preHex = getBackgroundHexFor(cmd.getId()); // expect this helper to exist
            colorCombo.setSelectedItem(preHex == null ? "" : preHex);
            colorCombo.setPreferredSize(new Dimension(72, 24));
            colorSelectors.put(cmd, colorCombo);

            // Right side checkbox (optional)
            boolean preRight = config.rightSideIds.contains(cmd.getId());
            JCheckBox rightChk = new JCheckBox("rechts", preRight);
            rightChk.setToolTipText("Button rechts anordnen");
            rightSideChecks.put(cmd, rightChk);

            // Order spinner: use stored order or suggest next free; step size = 1
            int currentOrder = getOrderFor(cmd.getId()); // expect this helper to exist
            int initialOrder;
            if (currentOrder > 0) {
                initialOrder = currentOrder;
            } else {
                initialOrder = nextFreeOrder;
                usedOrders.add(Integer.valueOf(initialOrder));
                // advance next free
                nextFreeOrder++;
                while (usedOrders.contains(Integer.valueOf(nextFreeOrder))) nextFreeOrder++;
            }
            JSpinner orderSpin = new JSpinner(new SpinnerNumberModel(initialOrder, 1, 9999, 1));
            orderSpin.setPreferredSize(new Dimension(64, 24));
            orderSpinners.put(cmd, orderSpin);

            checkboxes.put(cmd, box);

            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
            right.add(new JLabel("Pos:"));
            right.add(orderSpin);
            right.add(iconCombo);
            right.add(colorCombo);
            right.add(rightChk);

            line.add(box, BorderLayout.CENTER);
            line.add(right, BorderLayout.EAST);
            commandPanel.add(line);
        }

        JPanel fullPanel = new JPanel(new BorderLayout(8, 8));
        fullPanel.add(new JScrollPane(commandPanel), BorderLayout.CENTER);

        JPanel sizePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        sizePanel.add(new JLabel("Buttongröße:"));
        JSpinner sizeSpinner = new JSpinner(new SpinnerNumberModel(config.buttonSizePx, 24, 128, 4));
        sizePanel.add(sizeSpinner);
        sizePanel.add(new JLabel("Schrift %:"));
        JSpinner ratioSpinner = new JSpinner(new SpinnerNumberModel(config.fontSizeRatio, 0.3, 1.0, 0.05));
        sizePanel.add(ratioSpinner);

        fullPanel.add(sizePanel, BorderLayout.SOUTH);

        Object[] options = new Object[] { "Standard laden", "OK", "Abbrechen" };
        int result = JOptionPane.showOptionDialog(
                this,
                fullPanel,
                "Toolbar konfigurieren",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[1] // default: "OK"
        );

        if (result == 0) {
            // Load default buttons (keep sizes/rightSideIds)
            config.buttons = buildDefaultButtonsForAllCommands();
            saveToolbarSettings();
            rebuildButtons();
            return;
        }

        if (result == 1) {
            // Build new button list and validate unique positions
            config.buttons.clear();

            // Track duplicates: position -> ids
            Map<Integer, java.util.List<String>> byPos = new LinkedHashMap<Integer, java.util.List<String>>();
            LinkedHashSet<String> newRight = new LinkedHashSet<String>();

            for (Map.Entry<MenuCommand, JCheckBox> entry : checkboxes.entrySet()) {
                MenuCommand cmd = entry.getKey();
                boolean selected = entry.getValue().isSelected();
                if (!selected) continue;

                String icon = Objects.toString(iconSelectors.get(cmd).getSelectedItem(), "").trim();

                // Normalize hex color (allow empty)
                String hex = Objects.toString(colorSelectors.get(cmd).getSelectedItem(), "").trim();
                if (hex.length() > 0 && !hex.startsWith("#")) hex = "#" + hex;
                if (hex.length() == 0) hex = null;

                int pos = ((Number) orderSpinners.get(cmd).getValue()).intValue();

                ToolbarButtonConfig tbc = new ToolbarButtonConfig(cmd.getId(), icon);
                tbc.backgroundHex = hex;         // keep color
                tbc.order = Integer.valueOf(pos); // store order
                config.buttons.add(tbc);

                java.util.List<String> ids = byPos.get(Integer.valueOf(pos));
                if (ids == null) {
                    ids = new ArrayList<String>();
                    byPos.put(Integer.valueOf(pos), ids);
                }
                ids.add(cmd.getId());

                if (rightSideChecks.get(cmd).isSelected()) {
                    newRight.add(cmd.getId());
                }
            }

            // Reject duplicates
            java.util.List<Integer> dupPositions = new ArrayList<Integer>();
            for (Map.Entry<Integer, java.util.List<String>> e : byPos.entrySet()) {
                if (e.getValue().size() > 1) {
                    dupPositions.add(e.getKey());
                }
            }
            if (!dupPositions.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        "Doppelte Positionswerte sind nicht zulässig: " + dupPositions,
                        "Ungültige Reihenfolge",
                        JOptionPane.ERROR_MESSAGE
                );
                return; // do not save partial state
            }

            // Persist sizes "wie im Original"
            config.buttonSizePx = (Integer) sizeSpinner.getValue();
            config.fontSizeRatio = ((Double) ratioSpinner.getValue()).floatValue();

            // Persist right-side IDs
            config.rightSideIds = newRight;

            saveToolbarSettings();
            rebuildButtons();
        }
    }

    // NEU: Bereits gespeicherte Hintergrundfarbe für ein Command ermitteln
    private String getBackgroundHexFor(String id) {
        for (ToolbarButtonConfig b : config.buttons) {
            if (b.id.equals(id)) {
                // Requires: add 'public String backgroundHex;' to ToolbarButtonConfig
                return b.backgroundHex;
            }
        }
        return null;
    }

    private boolean isCommandActive(String id) {
        return config.buttons.stream().anyMatch(b -> b.id.equals(id));
    }

    private String getIconFor(String id) {
        return config.buttons.stream()
                .filter(b -> b.id.equals(id))
                .map(b -> b.icon)
                .findFirst()
                .orElse("🔘");
    }

    private void loadToolbarSettings() {
        Path file = Paths.get(System.getProperty("user.home"), ".wd4j", "toolbar.json");

        // Case 1: No file yet -> initialize with defaults (not empty!)
        if (!Files.exists(file)) {
            config = createDefaultConfigWithButtons();
            return;
        }

        // Case 2: Load file; if broken or empty, heal to defaults
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            config = gson.fromJson(reader, ToolbarConfig.class);
            ensureConfig();
            if (config.buttons == null || config.buttons.isEmpty()) {
                // Heal empty list that could be written by older versions
                config.buttons = buildDefaultButtonsForAllCommands();
                saveToolbarSettings(); // persist healing once
            }
            if (config.buttonSizePx <= 0) config.buttonSizePx = 48;
            if (config.fontSizeRatio <= 0f) config.fontSizeRatio = 0.75f;
        } catch (IOException e) {
            System.err.println("⚠️ Fehler beim Laden der Toolbar-Konfiguration: " + e.getMessage());
            config = createDefaultConfigWithButtons();
        }
    }

    private void saveToolbarSettings() {
        Path file = Paths.get(System.getProperty("user.home"), ".wd4j", "toolbar.json");
        try {
            Files.createDirectories(file.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                gson.toJson(config, writer);
            }
        } catch (IOException e) {
            System.err.println("⚠️ Fehler beim Speichern der Toolbar-Konfiguration: " + e.getMessage());
        }
    }

    private String[] getSimpleIconSuggestions() {
        return new String[] {
                // System & Dateien
                "💾", "📁", "📂", "📄", "📃", "📜", "🗃", "🗄", "📇", "📑", "📋", "🗂", "📦", "📬", "📮", "📬", "📪", "📭",

                // Aktionen
                "✔", "❌", "✖", "✅", "✳", "✴", "➕", "➖", "➗", "✂", "🔀", "🔁", "🔂", "🔄", "🔃", "🔽", "🔼", "⬅", "➡", "⬆", "⬇",

                // Navigation
                "🔙", "🔚", "🔛", "🔜", "🔝", "⬅", "➡", "⏮", "⏭", "⏫", "⏬", "⏪", "⏩",

                // Status / Anzeigen
                "🆗", "🆕", "🆙", "🆒", "🆓", "🆖", "🈚", "🈶", "🈸", "🈺", "🈹", "🈯",

                // Zeit
                "⏰", "⏱", "⏲", "🕛", "🕧", "🕐", "🕜", "🕑", "🕝", "🕒", "🕞", "🕓", "🕟", "🕔", "🕠", "🕕", "🕡", "🕖", "🕢", "🕗", "🕣", "🕘", "🕤", "🕙", "🕥", "🕚", "🕦", "🕮",

                // Kommunikation
                "📩", "📨", "📧", "📫", "📪", "📬", "📭", "📮", "✉", "🔔", "🔕", "📢", "📣", "📡",

                // Werkzeuge
                "🔧", "🔨", "🪛", "🪚", "🛠", "🧰", "🔩", "⚙", "🧲", "🔗", "📎", "🖇",

                // Texteingabe / Bearbeitung
                "📝", "✏", "✒", "🖊", "🖋", "🖌", "🔤", "🔡", "🔠", "🔣", "🔠",

                // Sonstiges Nützliches
                "🔍", "🔎", "🔒", "🔓", "🔑", "🗝", "📌", "📍", "📏", "📐", "📊", "📈", "📉", "📅", "📆", "🗓", "📇", "🧾", "📖", "📚",

                // Personen-/Datenkontext
                "🧑", "👤", "👥", "🧠", "🦷", "🫀", "🫁",

                // Code / IT
                "💻", "🖥", "🖨", "⌨", "🖱", "🖲", "💽", "💾", "💿", "📀", "🧮", "📡",

                // Hilfe / Info / System
                "ℹ", "❓", "❗", "‼", "⚠", "🚫", "🔞", "♻", "⚡", "🔥", "💡", "🔋", "🔌", "🧯",

                // Symbole / Stil
                "🔘", "🔴", "🟢", "🟡", "🟠", "🔵", "🟣", "⚫", "⚪", "🟥", "🟧", "🟨", "🟩", "🟦", "🟪", "⬛", "⬜",

                // Buchstaben-/Zahlenrahmen
                "🅰", "🅱", "🆎", "🅾", "🔠", "🔢", "🔣", "🔤"
        };
    }

    /** Ensure config object exists and fields are sane. */
    // ÄNDERT: rightSideIds sicher initialisieren (falls null)
    private void ensureConfig() {
        if (config == null) {
            config = createDefaultConfigWithButtons();
            return;
        }
        if (config.buttons == null) {
            config.buttons = buildDefaultButtonsForAllCommands();
        }
        if (config.buttons.isEmpty()) {
            config.buttons = buildDefaultButtonsForAllCommands();
        }
        if (config.buttonSizePx <= 0) config.buttonSizePx = 48;
        if (config.fontSizeRatio <= 0f) config.fontSizeRatio = 0.75f;

        // NEW: initialize right side set
        if (config.rightSideIds == null) {
            config.rightSideIds = new LinkedHashSet<String>(); // keep user order when editing JSON
        }
    }

    // NEU: Prüfe, ob eine ID rechts ausgerichtet werden soll
    private boolean isRightAligned(String id) {
        if (config == null || config.rightSideIds == null || id == null) return false;
        return config.rightSideIds.contains(id);
    }

    /** Create a fully initialized config with non-empty default buttons. */
    private ToolbarConfig createDefaultConfigWithButtons() {
        ToolbarConfig cfg = new ToolbarConfig();
        cfg.buttonSizePx = 48;
        cfg.fontSizeRatio = 0.75f;
        cfg.buttons = buildDefaultButtonsForAllCommands(); // never start empty
        return cfg;
    }

    // ÄNDERT: Standardliste setzt nun Default-Hintergründe für Record/Play
    // ÄNDERT: Default-Buttons mit sequentieller Standard-Reihenfolge versehen (1..n)
    private List<ToolbarButtonConfig> buildDefaultButtonsForAllCommands() {
        List<ToolbarButtonConfig> list = new ArrayList<ToolbarButtonConfig>();
        List<MenuCommand> all = new ArrayList<MenuCommand>(CommandRegistryImpl.getInstance().getAll());
        Collections.sort(all, new Comparator<MenuCommand>() {
            @Override public int compare(MenuCommand a, MenuCommand b) {
                return a.getLabel().compareToIgnoreCase(b.getLabel());
            }
        });
        int pos = 1;
        for (MenuCommand cmd : all) {
            ToolbarButtonConfig tbc = new ToolbarButtonConfig(cmd.getId(), defaultIconFor(cmd));
            tbc.order = Integer.valueOf(pos++); // Requires: 'public Integer order;' in ToolbarButtonConfig
            // Keep your default backgrounds if you use them
            String bg = defaultBackgroundHexFor(cmd);
            if (bg != null) tbc.backgroundHex = bg;
            list.add(tbc);
        }
        return list;
    }

    // NEU: Hintergrund pro Command ableiten (Record=rot, Play=grün, sonst null)
    private String defaultBackgroundHexFor(MenuCommand cmd) {
        // Implement simple rules without changing existing semantics
        String id = (cmd.getId() == null) ? "" : cmd.getId().toLowerCase(Locale.ROOT);
        if (id.contains("record")) return "#FF0000";        // red
        if (id.contains("testsuite.play") || id.contains("play")) return "#00AA00"; // green
        return null; // keep default look
    }

    /** Map command IDs to legible, monochrome-friendly Unicode icons. */
    private String defaultIconFor(MenuCommand cmd) {
        String id = (cmd.getId() == null) ? "" : cmd.getId().toLowerCase(Locale.ROOT);

        // Playback / Recording
        if (id.contains("record"))                                     return "⦿";
        if (id.contains("testsuite.play") || id.contains("play"))      return "▶";
        if (id.contains("testsuite.stop") || id.contains("stop"))      return "■";

        // Browser / Tabs / Navigation
        if (id.contains("browser.launch") || id.contains("launch"))    return "🌐";
        if (id.contains("terminate"))                                  return "■";
        if (id.contains("newtab"))                                     return "＋";
        if (id.contains("closetab") || id.contains("close"))           return "✖";
        if (id.contains("reload") || id.contains("refresh"))           return "↻";
        if (id.contains("back"))                                       return "←";
        if (id.contains("forward"))                                    return "→";
        if (id.contains("home"))                                       return "🏠";

        // Tools
        if (id.contains("screenshot") || id.contains("capture"))       return "📷";
        if (id.contains("selectors"))                                   return "🔍";
        if (id.contains("domevents"))                                   return "📜";

        // User / Login
        if (id.contains("userselection") || id.contains("userregistry")) return "📇";
        if (id.contains("login") || id.contains("otp"))                 return "🔑";

        // View / Drawer
        if (id.contains("view.toggleleft"))                            return "⟨";
        if (id.contains("view.toggleright"))                           return "⟩";

        // Settings / Shortcuts
        if (id.contains("settings") || id.contains("configure"))       return "⚙";
        if (id.contains("shortcut"))                                   return "⌘";

        // Fallback
        return "●";
    }

    // NEU: Liefere gespeicherte Position, 0 wenn unbekannt (wird als "am Ende" behandelt)
    private int getOrderFor(String id) {
        if (config == null || config.buttons == null) return 0;
        for (ToolbarButtonConfig b : config.buttons) {
            if (id.equals(b.id)) {
                return (b.order == null) ? 0 : b.order.intValue();
            }
        }
        return 0;
    }

    // NEU: Vergleich nach 'order' (kleiner zuerst), dann Label zur Stabilität
    private Comparator<ToolbarButtonConfig> buttonOrderComparator() {
        return new Comparator<ToolbarButtonConfig>() {
            @Override public int compare(ToolbarButtonConfig a, ToolbarButtonConfig b) {
                int oa = normalizeOrder(a.order);
                int ob = normalizeOrder(b.order);
                if (oa != ob) return (oa < ob) ? -1 : 1;
                String la = labelOf(a.id);
                String lb = labelOf(b.id);
                return la.compareToIgnoreCase(lb);
            }
        };
    }

    // NEU: Behandle fehlende/ungültige Orders als "sehr groß" -> am Ende einsortieren
    private int normalizeOrder(Integer ord) {
        if (ord == null) return Integer.MAX_VALUE;
        int v = ord.intValue();
        return (v <= 0) ? Integer.MAX_VALUE : v;
    }

    // NEU: Hole Label zur stabilen Zweitsortierung
    private String labelOf(String id) {
        if (id == null) return "";
        java.util.Optional<MenuCommand> mc = CommandRegistryImpl.getInstance().getById(id);
        return mc.isPresent() ? mc.get().getLabel() : id;
    }
}
