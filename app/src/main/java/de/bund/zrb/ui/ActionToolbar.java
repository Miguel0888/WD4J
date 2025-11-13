package de.bund.zrb.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.bund.zrb.service.SettingsService;
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

    private static final String SUPPRESS_CLICK_PROP = "suppressNextClick";

    public ActionToolbar() {
        setFloatable(false);

        loadToolbarSettings();
        rebuildButtons();
    }

    // ÄNDERT: Hintergrundfarbe anwenden (Button oder Gruppenfarbe)
    // ÄNDERT: Buttons je nach ID-Set links oder rechts platzieren
    // ÄNDERT: Buttons nach 'order' sortieren
    // ÄNDERT: Buttons mit DnD ausstatten und Drop-Handler an Panels binden
    private void rebuildButtons() {
        removeAll();

        final JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        final JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 2));

        if (config != null && config.buttons != null) {
            // Links
            java.util.List<ToolbarButtonConfig> left = new ArrayList<ToolbarButtonConfig>();
            // Rechts
            java.util.List<ToolbarButtonConfig> right = new ArrayList<ToolbarButtonConfig>();

            for (ToolbarButtonConfig b : config.buttons) {
                if (isRightAligned(b.id)) right.add(b); else left.add(b);
            }
            Collections.sort(left, buttonOrderComparator());
            Collections.sort(right, buttonOrderComparator());

            // Build helper to create a button with DnD
            createButtonsIntoPanel(leftPanel, left);
            createButtonsIntoPanel(rightPanel, right);
        }

        // ⚙-Button immer ganz rechts (nicht ziehbar)
        JButton configBtn = new JButton(cp(0x1F6E0)); // 🛠 (Text-Style)
        configBtn.setName("gearButton"); // help drop handler to ignore this one
        configBtn.setMargin(new Insets(0, 0, 0, 0));
        configBtn.setToolTipText("Toolbar anpassen");
        configBtn.setPreferredSize(new Dimension(config.buttonSizePx, config.buttonSizePx));
        int fontSize = (int) (config.buttonSizePx * config.fontSizeRatio);
        configBtn.setFont(configBtn.getFont().deriveFont((float) fontSize));
        configBtn.setFocusPainted(false);
        configBtn.addActionListener(e -> openConfigDialog());
        rightPanel.add(configBtn);

        // Neuer Defaults-Button (Zurücksetzen auf zentrale Standardbelegung)
        JButton defaultsBtn = new JButton("↺");
        defaultsBtn.setToolTipText("Auf Standard-Toolbar zurücksetzen");
        defaultsBtn.setMargin(new Insets(0,0,0,0));
        defaultsBtn.setPreferredSize(new Dimension(config.buttonSizePx, config.buttonSizePx));
        defaultsBtn.setFont(defaultsBtn.getFont().deriveFont((float) fontSize));
        defaultsBtn.setFocusPainted(false);
        defaultsBtn.addActionListener(ev -> {
            // Erzeuge frische Default-Konfiguration (inkl. Buttons)
            ToolbarConfig fresh = createDefaultConfigWithButtons();
            // Bewahre evtl. vorhandene Hidden-IDs/Gruppenfarben wenn sinnvoll
            if (config != null) {
                fresh.hiddenCommandIds = (config.hiddenCommandIds != null) ? new LinkedHashSet<>(config.hiddenCommandIds) : new LinkedHashSet<>();
                fresh.groupColors = (config.groupColors != null) ? new LinkedHashMap<>(config.groupColors) : new LinkedHashMap<>();
            }
            config = fresh;
            saveToolbarSettings();
            rebuildButtons();
        });
        rightPanel.add(defaultsBtn);

        // Enable drops on panels (left/right)
        leftPanel.setTransferHandler(new ToolbarPanelDropHandler(false));
        rightPanel.setTransferHandler(new ToolbarPanelDropHandler(true));

        setLayout(new BorderLayout());
        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.EAST);

        revalidate();
        repaint();
    }

    /** Create JButtons with drag capability and add to given panel. */
    private void createButtonsIntoPanel(final JPanel panel, final java.util.List<ToolbarButtonConfig> list) {
        for (final ToolbarButtonConfig btnCfg : list) {
            CommandRegistryImpl.getInstance().getById(btnCfg.id).ifPresent(cmd -> {
                JButton btn = new JButton(btnCfg.icon);
                btn.putClientProperty("cmdId", btnCfg.id);
                btn.putClientProperty(SUPPRESS_CLICK_PROP, Boolean.FALSE); // initialize flag
                btn.setMargin(new Insets(0, 0, 0, 0));
                btn.setToolTipText(cmd.getLabel());
                btn.setPreferredSize(new Dimension(config.buttonSizePx, config.buttonSizePx));

                int fontSize = (int) (config.buttonSizePx * config.fontSizeRatio);
                btn.setFont(btn.getFont().deriveFont((float) fontSize));
                btn.setFocusPainted(false);

                // 1) Button-spezifische Farbe
                boolean colored = false;
                if (btnCfg.backgroundHex != null && btnCfg.backgroundHex.trim().length() > 0) {
                    try {
                        String hex = btnCfg.backgroundHex.trim();
                        if (!hex.startsWith("#")) hex = "#" + hex;
                        btn.setOpaque(true);
                        btn.setContentAreaFilled(true);
                        btn.setBackground(Color.decode(hex));
                        colored = true;
                    } catch (NumberFormatException ignore) { /* fallback auf Gruppe unten */ }
                }
                // 2) Fallback: Gruppenfarbe (Prefix vor '.')
                if (!colored) {
                    String groupHex = getGroupColorFor(btnCfg.id);
                    if (groupHex != null && groupHex.trim().length() > 0) {
                        try {
                            String hex = groupHex.trim();
                            if (!hex.startsWith("#")) hex = "#" + hex;
                            btn.setOpaque(true);
                            btn.setContentAreaFilled(true);
                            btn.setBackground(Color.decode(hex));
                        } catch (NumberFormatException ignore) { /* keine Färbung */ }
                    }
                }

                // Action: perform only if no drag happened
                btn.addActionListener(new java.awt.event.ActionListener() {
                    @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                        JComponent src = (JComponent) e.getSource();
                        boolean suppress = Boolean.TRUE.equals(src.getClientProperty(SUPPRESS_CLICK_PROP));
                        if (suppress) {
                            // Reset flag and do not perform action
                            src.putClientProperty(SUPPRESS_CLICK_PROP, Boolean.FALSE);
                            return;
                        }
                        cmd.perform();
                    }
                });

                // Enable drag with movement threshold
                btn.setTransferHandler(new DragButtonTransferHandler());
                DragInitiatorMouseAdapter dima = new DragInitiatorMouseAdapter();
                btn.addMouseListener(dima);
                btn.addMouseMotionListener(dima);

                panel.add(btn);
            });
        }
    }

    // --- Gruppierungs-Helfer: Farbe per Gruppen-Prefix (vor dem ersten Punkt) ---
    private String getGroupColorFor(String commandId) {
        if (commandId == null || config == null || config.groupColors == null) return null;
        int dot = commandId.indexOf('.');
        String grp = (dot > 0) ? commandId.substring(0, dot) : "(ohne)";
        return config.groupColors.get(grp);
    }

    // NEU: starte Drag erst ab Bewegungsschwelle; blockiere Clicks nicht
    private static final class DragInitiatorMouseAdapter extends java.awt.event.MouseAdapter {
        private static final int DRAG_THRESHOLD_PX = 5;

        private java.awt.Point pressPoint;
        private boolean dragStarted = false;

        @Override public void mousePressed(java.awt.event.MouseEvent e) {
            pressPoint = e.getPoint();
            dragStarted = false;
        }

        @Override public void mouseDragged(java.awt.event.MouseEvent e) {
            if (dragStarted || pressPoint == null) return;

            int dx = Math.abs(e.getX() - pressPoint.x);
            int dy = Math.abs(e.getY() - pressPoint.y);
            if (dx >= DRAG_THRESHOLD_PX || dy >= DRAG_THRESHOLD_PX) {
                JComponent c = (JComponent) e.getSource();
                // Mark next click as suppressed
                c.putClientProperty(SUPPRESS_CLICK_PROP, Boolean.TRUE);

                // Disarm button so Swing does not fire action on release
                if (c instanceof AbstractButton) {
                    ButtonModel m = ((AbstractButton) c).getModel();
                    m.setArmed(false);
                    m.setPressed(false);
                }

                TransferHandler th = c.getTransferHandler();
                if (th != null) {
                    th.exportAsDrag(c, e, TransferHandler.MOVE);
                    dragStarted = true;
                }
            }
        }

        @Override public void mouseReleased(java.awt.event.MouseEvent e) {
            pressPoint = null;
            dragStarted = false;
        }
    }

    /** Handle drops onto a toolbar panel; reorder and update config accordingly. */
    private final class ToolbarPanelDropHandler extends TransferHandler {
        private final boolean toRightSide;

        ToolbarPanelDropHandler(boolean toRightSide) {
            this.toRightSide = toRightSide;
        }

        @Override public boolean canImport(TransferSupport support) {
            // Accept only string flavor (command ID) and move action
            if (!support.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.stringFlavor)) return false;
            support.setDropAction(TransferHandler.MOVE);
            return true;
        }

        @Override public boolean importData(TransferSupport support) {
            if (!canImport(support)) return false;

            try {
                String movedId = (String) support.getTransferable()
                        .getTransferData(java.awt.datatransfer.DataFlavor.stringFlavor);

                if (movedId == null || movedId.trim().length() == 0) return false;

                // Compute insert index relative to visible buttons (ignore gear)
                JComponent dropTarget = (JComponent) support.getComponent();
                java.awt.Point p = support.getDropLocation().getDropPoint();
                int insertIndex = computeInsertIndex((JPanel) dropTarget, p);

                // Apply model changes and persist
                updateModelAfterDrop(movedId, toRightSide, insertIndex, (JPanel) dropTarget);

                // Rebuild toolbar to reflect new order
                rebuildButtons();
                return true;
            } catch (Exception ex) {
                // Be defensive but keep UI responsive
                System.err.println("⚠️ DnD-Import fehlgeschlagen: " + ex.getMessage());
                return false;
            }
        }
    }

    /** Export the command ID of a button as a string during drag. */
    private static final class DragButtonTransferHandler extends TransferHandler {
        @Override protected java.awt.datatransfer.Transferable createTransferable(JComponent c) {
            Object id = (c instanceof JButton) ? ((JButton) c).getClientProperty("cmdId") : null;
            String s = (id == null) ? "" : String.valueOf(id);
            return new java.awt.datatransfer.StringSelection(s);
        }
        @Override public int getSourceActions(JComponent c) { return MOVE; }
    }

    /** Compute drop insertion index within panel based on mouse point (ignore gear button). */
    private int computeInsertIndex(JPanel panel, java.awt.Point dropPoint) {
        Component[] comps = panel.getComponents();
        int effectiveCount = 0;
        for (int i = 0; i < comps.length; i++) {
            if (isGearButton(comps[i])) continue;
            Rectangle r = comps[i].getBounds();
            int midX = r.x + r.width / 2;
            if (dropPoint.x <= midX) {
                return effectiveCount;
            }
            effectiveCount++;
        }
        return effectiveCount; // append at end
    }

    /** Update config after a drop: move ID to target side, adjust orders within both affected sides, persist. */
    private void updateModelAfterDrop(String movedId, boolean targetRightSide, int insertIndex, JPanel targetPanel) {
        if (config == null || config.buttons == null) return;

        // Ensure rightSideIds exists
        if (config.rightSideIds == null) {
            config.rightSideIds = new LinkedHashSet<String>();
        }

        // Determine current side of the moved button
        boolean wasRight = isRightAligned(movedId);

        // Move side membership if needed
        if (targetRightSide && !wasRight) {
            config.rightSideIds.add(movedId);
        } else if (!targetRightSide && wasRight) {
            config.rightSideIds.remove(movedId);
        }

        // Recompute order for TARGET side from current UI order + inserted element
        java.util.List<String> targetIds = extractIdsFromPanel(targetPanel); // visible order
        // Insert moved ID at computed index (remove first if already listed, for cross-panel moves)
        targetIds.remove(movedId);
        if (insertIndex < 0) insertIndex = 0;
        if (insertIndex > targetIds.size()) insertIndex = targetIds.size();
        targetIds.add(insertIndex, movedId);
        // Write back order 1..n for target side
        applySequentialOrderToIds(targetIds);

        // Recompute order for SOURCE side if side changed
        if (wasRight != targetRightSide) {
            JPanel otherPanel = findSiblingPanel(targetPanel);
            if (otherPanel != null) {
                java.util.List<String> sourceIds = extractIdsFromPanel(otherPanel);
                // The movedId was removed visually already (drag source), but ensure
                sourceIds.remove(movedId);
                applySequentialOrderToIds(sourceIds);
            }
        }

        // Persist
        saveToolbarSettings();
    }

    /** Extract the ordered list of command IDs from a panel (ignore gear). */
    private java.util.List<String> extractIdsFromPanel(JPanel panel) {
        java.util.List<String> ids = new ArrayList<String>();
        Component[] comps = panel.getComponents();
        for (int i = 0; i < comps.length; i++) {
            if (!(comps[i] instanceof JButton)) continue;
            if (isGearButton(comps[i])) continue;
            Object id = ((JButton) comps[i]).getClientProperty("cmdId");
            if (id != null) {
                ids.add(String.valueOf(id));
            }
        }
        return ids;
    }

    /** Assign orders 1..n to given IDs within config.buttons (others remain unchanged). */
    private void applySequentialOrderToIds(java.util.List<String> orderedIds) {
        int pos = 1;
        for (String id : orderedIds) {
            ToolbarButtonConfig cfg = findCfgById(id);
            if (cfg != null) {
                cfg.order = Integer.valueOf(pos);
                pos++;
            }
        }
    }

    /** Find the opposite panel (left/right) given the target panel by walking the container. */
    private JPanel findSiblingPanel(JPanel targetPanel) {
        Container parent = getParent();
        if (!(getLayout() instanceof BorderLayout)) return null;
        // Our toolbar adds WEST (leftPanel) and EAST (rightPanel); walk components
        for (Component c : getComponents()) {
            if (c instanceof JPanel && c != targetPanel) {
                return (JPanel) c;
            }
        }
        return null;
    }

    /** Detect the gear button to exclude it from DnD order calculations. */
    private boolean isGearButton(Component c) {
        if (!(c instanceof JButton)) return false;
        JButton b = (JButton) c;
        if ("gearButton".equals(b.getName())) return true;
        // Fallback: identify by label; keep robust for existing code
        return "⚙".equals(b.getText());
    }

    /** Lookup ToolbarButtonConfig by command ID. */
    private ToolbarButtonConfig findCfgById(String id) {
        if (id == null || config == null || config.buttons == null) return null;
        for (ToolbarButtonConfig b : config.buttons) {
            if (id.equals(b.id)) return b;
        }
        return null;
    }

    // --- NEU: Dialog via ausgelagerter Klasse öffnen ---
    private void openConfigDialog() {
        ToolbarConfigDialog dlg = new ToolbarConfigDialog(
                SwingUtilities.getWindowAncestor(this),
                this.config,
                new ArrayList<MenuCommand>(CommandRegistryImpl.getInstance().getAll()),
                getSimpleIconSuggestions()
        );
        ToolbarConfig updated = dlg.showDialog();
        if (updated != null) {
            this.config = updated;
            saveToolbarSettings();
            rebuildButtons();
        }
    }

    private void loadToolbarSettings() {
        Path file = Paths.get(System.getProperty("user.home"), SettingsService.getInstance().APP_FOLDER, "toolbar.json");

        if (!Files.exists(file)) {
            // Einmalig mit zentralen Defaults initialisieren
            List<MenuCommand> all = new ArrayList<>(CommandRegistryImpl.getInstance().getAll());
            config = ToolbarDefaults.createInitialConfig(all);
            saveToolbarSettings();
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            config = gson.fromJson(reader, ToolbarConfig.class);
            ensureConfig(); // heilt Felder
            // Heilung alter/leer gespeicherter Buttons -> zentrale Defaults
            if (config.buttons == null || config.buttons.isEmpty()) {
                List<MenuCommand> all = new ArrayList<>(CommandRegistryImpl.getInstance().getAll());
                config = ToolbarDefaults.createInitialConfig(all);
                saveToolbarSettings();
            }
            if (config.buttonSizePx <= 0) config.buttonSizePx = 48;
            if (config.fontSizeRatio <= 0f) config.fontSizeRatio = 0.75f;
        } catch (IOException e) {
            System.err.println("⚠️ Fehler beim Laden der Toolbar-Konfiguration: " + e.getMessage());
            List<MenuCommand> all = new ArrayList<>(CommandRegistryImpl.getInstance().getAll());
            config = ToolbarDefaults.createInitialConfig(all);
        }
    }

    private void ensureConfig() {
        if (config == null) {
            List<MenuCommand> all = new ArrayList<>(CommandRegistryImpl.getInstance().getAll());
            config = ToolbarDefaults.createInitialConfig(all);
            return;
        }
        if (config.buttons == null || config.buttons.isEmpty()) {
            List<MenuCommand> all = new ArrayList<>(CommandRegistryImpl.getInstance().getAll());
            config = ToolbarDefaults.createInitialConfig(all);
        }
        if (config.buttonSizePx <= 0) config.buttonSizePx = 48;
        if (config.fontSizeRatio <= 0f) config.fontSizeRatio = 0.75f;
        if (config.rightSideIds == null) config.rightSideIds = new LinkedHashSet<>();
        if (config.groupColors == null)  config.groupColors  = new LinkedHashMap<>();
        if (config.hiddenCommandIds == null) config.hiddenCommandIds = new LinkedHashSet<>();
    }

    private void saveToolbarSettings() {
        Path file = Paths.get(System.getProperty("user.home"), SettingsService.getInstance().APP_FOLDER, "toolbar.json");
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
        cfg.rightSideIds = new LinkedHashSet<String>();
        cfg.groupColors = new LinkedHashMap<String, String>();
        return cfg;
    }

    // Default-Buttons (mit Order & ggf. Default-Farbe)
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
            String bg = defaultBackgroundHexFor(cmd);
            if (bg != null) tbc.backgroundHex = bg;
            list.add(tbc);
        }
        return list;
    }

    private String defaultBackgroundHexFor(MenuCommand cmd) {
        String id = (cmd.getId() == null) ? "" : cmd.getId().toLowerCase(Locale.ROOT);
        if (id.contains("record")) return "#FF0000";                       // rot
        if (id.contains("testsuite.play") || id.contains("play")) return "#00AA00"; // grün
        return null;
    }

    /** Map command IDs to legible, monochrome-friendly Unicode icons. */
    private String defaultIconFor(MenuCommand cmd) {
        String id = (cmd.getId() == null) ? "" : cmd.getId().toLowerCase(Locale.ROOT);

        // Playback / Recording
        if (id.contains("record.play"))      return "▶";
        if (id.contains("record.stop"))      return "■";
        if (id.contains("record.toggle"))    return "⦿";
        if (id.contains("testsuite.play"))   return "▶";
        if (id.contains("testsuite.stop"))   return "■";

        // Browser / Tabs / Navigation
        if (id.contains("browser.launch") || id.contains("launch")) return cp(0x1F310); // 🌐
        if (id.contains("terminate"))                                return "■";
        if (id.contains("newtab"))                                   return "＋";
        if (id.contains("closetab") || id.contains("close"))         return "✖";
        if (id.contains("reload") || id.contains("refresh"))         return "↻";
        if (id.contains("back"))                                     return "←";
        if (id.contains("forward"))                                  return "→";
        if (id.contains("home"))                                     return cp(0x1F3E0); // 🏠

        // Tools
        if (id.contains("screenshot") || id.contains("capture"))     return cp(0x1F4F7); // 📷
        if (id.contains("selectors"))                                return cp(0x1F50D); // 🔍
        if (id.contains("domevents"))                                return cp(0x1F4DC); // 📜

        // User / Login
        if (id.contains("userselection") || id.contains("userregistry")) return cp(0x1F4C7); // 📇
        if (id.contains("login") || id.contains("otp"))                   return cp(0x1F511); // 🔑

        // View / Drawer
        if (id.contains("view.toggleleft"))                           return "⟨";
        if (id.contains("view.toggleright"))                          return "⟩";

        // Settings / Shortcuts
        if (id.contains("settings") || id.contains("configure"))      return "⚙";
        if (id.contains("shortcut"))                                  return "⌘";

        // Video / Recording-UI
        if (id.contains("video.toggle") || id.contains("video.record")) return new String(Character.toChars(0x1F3AC)); // 🎬

        // Fallback
        return "●";
    }

    // Liefere gespeicherte Position, 0 wenn unbekannt
    private int getOrderFor(String id) {
        if (config == null || config.buttons == null) return 0;
        for (ToolbarButtonConfig b : config.buttons) {
            if (id.equals(b.id)) {
                return (b.order == null) ? 0 : b.order.intValue();
            }
        }
        return 0;
    }

    // Vergleich nach 'order' (kleiner zuerst), dann Label
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

    // Behandle fehlende/ungültige Orders als "sehr groß"
    private int normalizeOrder(Integer ord) {
        if (ord == null) return Integer.MAX_VALUE;
        int v = ord.intValue();
        return (v <= 0) ? Integer.MAX_VALUE : v;
    }

    // Hole Label zur stabilen Zweitsortierung
    private String labelOf(String id) {
        if (id == null) return "";
        java.util.Optional<MenuCommand> mc = CommandRegistryImpl.getInstance().getById(id);
        return mc.isPresent() ? mc.get().getLabel() : id;
    }

    // erzeugt ein String aus einem Unicode-Codepoint (für Emoji > U+FFFF)
    private static String cp(int codePoint) {
        return new String(Character.toChars(codePoint));
    }
}
