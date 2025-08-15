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

    private void rebuildButtons() {
        removeAll();

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        for (ToolbarButtonConfig btnCfg : config.buttons) {
            CommandRegistryImpl.getInstance().getById(btnCfg.id).ifPresent(cmd -> {
                JButton btn = new JButton(btnCfg.icon);
                btn.setMargin(new Insets(0, 0, 0, 0)); // optional
//                btn.setBorder(BorderFactory.createEmptyBorder());
                btn.setToolTipText(cmd.getLabel());
                btn.setPreferredSize(new Dimension(config.buttonSizePx, config.buttonSizePx));

                int fontSize = (int) (config.buttonSizePx * config.fontSizeRatio);
                btn.setFont(btn.getFont().deriveFont((float) fontSize));

//                btn.setContentAreaFilled(false);
                btn.setFocusPainted(false);
//                btn.setBorder(BorderFactory.createLineBorder(Color.BLACK));

                btn.addActionListener(e -> cmd.perform());
                leftPanel.add(btn);
            });
        }

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 2));
        JButton configBtn = new JButton("⚙");
        configBtn.setMargin(new Insets(0, 0, 0, 0));
//        configBtn.setBorder(BorderFactory.createEmptyBorder());
        configBtn.setToolTipText("Toolbar anpassen");
        configBtn.setPreferredSize(new Dimension(config.buttonSizePx, config.buttonSizePx));
        configBtn.addActionListener(e -> openConfigDialog());

        int fontSize = (int) (config.buttonSizePx * config.fontSizeRatio);
        configBtn.setFont(configBtn.getFont().deriveFont((float) fontSize));
//        configBtn.setContentAreaFilled(false);
        configBtn.setFocusPainted(false);
//        configBtn.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        rightPanel.add(configBtn);

        setLayout(new BorderLayout());
        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.EAST);

        revalidate();
        repaint();
    }

    private void openConfigDialog() {
        List<MenuCommand> all = new ArrayList<>(CommandRegistryImpl.getInstance().getAll());
        Map<MenuCommand, JCheckBox> checkboxes = new LinkedHashMap<>();
        Map<MenuCommand, JComboBox<String>> iconSelectors = new LinkedHashMap<>();

        JPanel commandPanel = new JPanel(new GridLayout(0, 1));
        for (MenuCommand cmd : all) {
            JPanel line = new JPanel(new BorderLayout(4, 0));
            JCheckBox box = new JCheckBox(cmd.getLabel(), isCommandActive(cmd.getId()));

            JComboBox<String> iconCombo = new JComboBox<>(getSimpleIconSuggestions());
            iconCombo.setEditable(true);
            iconCombo.setSelectedItem(getIconFor(cmd.getId()));
            iconCombo.setPreferredSize(new Dimension(48, 24));
            iconSelectors.put(cmd, iconCombo);

            checkboxes.put(cmd, box);
            line.add(box, BorderLayout.CENTER);
            line.add(iconCombo, BorderLayout.EAST);
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

    // Add third button "Standard laden" (index 0), keep OK (index 1) and Cancel (index 2)
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
        // Load default buttons only; keep sizes intact
        config.buttons = buildDefaultButtonsForAllCommands();
        saveToolbarSettings();
        rebuildButtons();
        return;
    }

    if (result == 1) {
            config.buttons.clear();
            for (Map.Entry<MenuCommand, JCheckBox> entry : checkboxes.entrySet()) {
                if (entry.getValue().isSelected()) {
                    String icon = Objects.toString(iconSelectors.get(entry.getKey()).getSelectedItem(), "").trim();
                    config.buttons.add(new ToolbarButtonConfig(entry.getKey().getId(), icon));
                }
            }
            config.buttonSizePx = (Integer) sizeSpinner.getValue();
            config.fontSizeRatio = ((Double) ratioSpinner.getValue()).floatValue();

            saveToolbarSettings();
            rebuildButtons();
        }
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
        Path file = Paths.get(System.getProperty("user.home"), ".mainframemate", "toolbar.json");
        if (!Files.exists(file)) {
            config = new ToolbarConfig();
            config.buttonSizePx = 48;
            config.fontSizeRatio = 0.75f;
            config.buttons = new ArrayList<>();
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            config = gson.fromJson(reader, ToolbarConfig.class);
        } catch (IOException e) {
            System.err.println("⚠️ Fehler beim Laden der Toolbar-Konfiguration: " + e.getMessage());
            config = new ToolbarConfig();
        }
    }

    private void saveToolbarSettings() {
        Path file = Paths.get(System.getProperty("user.home"), ".mainframemate", "toolbar.json");
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
    }

    /** Create a fully initialized config with non-empty default buttons. */
    private ToolbarConfig createDefaultConfigWithButtons() {
        ToolbarConfig cfg = new ToolbarConfig();
        cfg.buttonSizePx = 48;
        cfg.fontSizeRatio = 0.75f;
        cfg.buttons = buildDefaultButtonsForAllCommands(); // never start empty
        return cfg;
    }

    private List<ToolbarButtonConfig> buildDefaultButtonsForAllCommands() {
        List<ToolbarButtonConfig> list = new ArrayList<ToolbarButtonConfig>();
        List<MenuCommand> all = new ArrayList<MenuCommand>(CommandRegistryImpl.getInstance().getAll());
        Collections.sort(all, new Comparator<MenuCommand>() {
            @Override public int compare(MenuCommand a, MenuCommand b) {
                return a.getLabel().compareToIgnoreCase(b.getLabel());
            }
        });
        for (MenuCommand cmd : all) {
            list.add(new ToolbarButtonConfig(cmd.getId(), defaultIconFor(cmd)));
        }
        return list;
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



}
