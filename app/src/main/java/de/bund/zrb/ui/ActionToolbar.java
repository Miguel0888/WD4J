package de.bund.zrb.ui;

import de.bund.zrb.service.SettingsService;
import de.bund.zrb.ui.commandframework.CommandRegistryImpl;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Einfache Toolbar mit persistenter Konfiguration (SettingsService, Key "toolbarConfig").
 * - Java 8 kompatibel (kein 'var', keine Records)
 * - Keine Abhängigkeit zu ToolbarConfig/ButtonConfig
 * - Reset auf Standard via ActionToolbar.resetToDefault()
 */
public class ActionToolbar extends JToolBar {
    private static final String SETTINGS_KEY = "toolbarConfig"; // v1|<btnSize>|<fontRatio>|id:icon,id2:icon2,...

    private static ActionToolbar instance;
    private final CommandRegistryImpl commandRegistry;
    private final Map<String, JButton> buttons = new HashMap<>();

    // einfache interne Konfig
    private static final class ButtonDef {
        String id;
        String icon;
        ButtonDef(String id, String icon) { this.id = id; this.icon = icon; }
    }
    private static final class SimpleConfig {
        List<ButtonDef> buttons = new ArrayList<>();
        int buttonSizePx = 32;
        double fontSizeRatio = 0.9;
    }

    private SimpleConfig currentConfig;

    public ActionToolbar(CommandRegistryImpl registry) {
        this.commandRegistry = registry;
        instance = this;
        setFloatable(false);
        loadToolbarSettings();
    }

    public static ActionToolbar getInstance() { return instance; }

    /** Setzt die Toolbar vollständig auf die Standardbelegung zurück und speichert sie. */
    public static void resetToDefault() {
        if (instance != null) {
            instance.currentConfig = createDefaultConfig();
            instance.saveToolbarSettings();
            instance.reload();
        }
    }

    /* ============================================================
       Laden/Speichern der Konfiguration
       ============================================================ */
    private void loadToolbarSettings() {
        try {
            String raw = SettingsService.getInstance().get(SETTINGS_KEY, String.class);
            if (raw == null || raw.trim().isEmpty()) {
                currentConfig = createDefaultConfig();
            } else {
                currentConfig = deserialize(raw);
                if (currentConfig == null || currentConfig.buttons == null || currentConfig.buttons.isEmpty()) {
                    currentConfig = createDefaultConfig();
                }
            }
        } catch (Exception ex) {
            currentConfig = createDefaultConfig();
        }
        reload();
    }

    private void saveToolbarSettings() {
        try {
            String raw = serialize(currentConfig);
            SettingsService.getInstance().set(SETTINGS_KEY, raw);
        } catch (Exception ignore) { /* best effort */ }
    }

    /* ============================================================
       Neuaufbau der Toolbar
       ============================================================ */
    private void reload() {
        removeAll();
        buttons.clear();

        if (currentConfig == null || currentConfig.buttons == null) {
            revalidate(); repaint();
            return;
        }

        int btnSize = Math.max(24, currentConfig.buttonSizePx);
        double fontRatio = Math.max(0.5, Math.min(2.0, currentConfig.fontSizeRatio));

        for (ButtonDef def : currentConfig.buttons) {
            final String id = def.id;
            final String icon = def.icon;

            if (id == null || id.trim().isEmpty()) continue;

            // Command finden und Button erzeugen
            java.util.Optional<de.bund.zrb.ui.commandframework.MenuCommand> opt =
                    commandRegistry.getById(id);
            if (!opt.isPresent()) continue;

            de.bund.zrb.ui.commandframework.MenuCommand cmd = opt.get();

            JButton b = new JButton(icon != null ? icon : "⬚");
            b.setFocusPainted(false);
            b.setToolTipText(cmd.getLabel());
            b.addActionListener(e -> cmd.perform());
            b.setPreferredSize(new Dimension(btnSize, btnSize));
            Font base = b.getFont();
            b.setFont(base.deriveFont((float)(base.getSize2D() * fontRatio)));

            add(b);
            buttons.put(id, b);
        }

        revalidate();
        repaint();
    }

    /* ============================================================
       Default-Konfiguration
       ============================================================ */
    /** Baut eine Default-Toolbar mit (nahezu) allen bekannten Commands auf. */
    private static SimpleConfig createDefaultConfig() {
        SimpleConfig cfg = new SimpleConfig();

        // Bevorzugte Icons (Unicode) pro Command-ID
        Map<String, String> preferredIcons = new HashMap<String, String>();
        preferredIcons.put("testsuite.play", "▶");
        preferredIcons.put("testsuite.stop", "■");
        preferredIcons.put("browser.launch", "🌐");
        preferredIcons.put("view.toggleLeftDrawer", "⟨");
        preferredIcons.put("view.toggleRightDrawer", "⟩");
        preferredIcons.put("file.configure", "⚙");

        // Alle Command-IDs beschaffen (robust via Reflection, Java 8)
        CommandRegistryImpl registry = CommandRegistryImpl.getInstance();
        List<String> allIds = safeAllCommandIds(registry);

        // Buttons in stabiler Reihenfolge
        for (String id : allIds) {
            String icon = preferredIcons.containsKey(id) ? preferredIcons.get(id) : "⬚";
            cfg.buttons.add(new ButtonDef(id, icon));
        }

        cfg.buttonSizePx = 32;
        cfg.fontSizeRatio = 0.9;
        return cfg;
    }

    /**
     * Versucht, alle IDs vom Registry zu bekommen:
     *  1) getAllCommandIds(): Collection<String>
     *  2) getAll(): Collection<MenuCommand> und jeweils getId()
     *  Fallback: kleine Liste.
     */
    private static List<String> safeAllCommandIds(CommandRegistryImpl registry) {
        try {
            // 1) getAllCommandIds()
            try {
                java.lang.reflect.Method m = registry.getClass().getMethod("getAllCommandIds");
                Object res = m.invoke(registry);
                if (res instanceof Collection) {
                    List<String> out = new ArrayList<String>();
                    for (Object o : (Collection<?>) res) if (o != null) out.add(String.valueOf(o));
                    Collections.sort(out);
                    return out;
                }
            } catch (NoSuchMethodException ignore) { /* weiter */ }

            // 2) getAll()
            try {
                java.lang.reflect.Method m = registry.getClass().getMethod("getAll");
                Object res = m.invoke(registry);
                if (res instanceof Collection) {
                    List<String> out = new ArrayList<String>();
                    for (Object cmd : (Collection<?>) res) {
                        try {
                            java.lang.reflect.Method mid = cmd.getClass().getMethod("getId");
                            Object id = mid.invoke(cmd);
                            if (id != null) out.add(String.valueOf(id));
                        } catch (Throwable ignoreEach) { /* weiter */ }
                    }
                    Collections.sort(out);
                    return out;
                }
            } catch (NoSuchMethodException ignore) { /* weiter */ }
        } catch (Throwable t) {
            // ignore: wir liefern Fallback
        }

        // Minimal-Fallback, falls API nichts davon bietet
        List<String> fallback = new ArrayList<String>();
        fallback.add("testsuite.play");
        fallback.add("testsuite.stop");
        fallback.add("browser.launch");
        fallback.add("view.toggleLeftDrawer");
        fallback.add("view.toggleRightDrawer");
        fallback.add("file.configure");
        return fallback;
    }

    /* ============================================================
       Mini-(De)Serialisierung ohne externe Libs
       Format v1:
         v1|<buttonSize>|<fontRatio>|id:icon,id2:icon2,...
       ============================================================ */
    private static String serialize(SimpleConfig cfg) {
        if (cfg == null) return "v1|32|0.9|";
        StringBuilder sb = new StringBuilder();
        sb.append("v1|").append(cfg.buttonSizePx).append("|").append(cfg.fontSizeRatio).append("|");
        boolean first = true;
        if (cfg.buttons != null) {
            for (ButtonDef b : cfg.buttons) {
                if (b == null || b.id == null) continue;
                if (!first) sb.append(',');
                first = false;
                sb.append(esc(b.id)).append(':').append(esc(b.icon == null ? "" : b.icon));
            }
        }
        return sb.toString();
    }

    private static SimpleConfig deserialize(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (!s.startsWith("v1|")) return null;
        String[] parts = s.split("\\|", 4);
        if (parts.length < 4) return null;

        SimpleConfig cfg = new SimpleConfig();
        try {
            cfg.buttonSizePx = Integer.parseInt(parts[1]);
        } catch (Exception ignore) { cfg.buttonSizePx = 32; }
        try {
            cfg.fontSizeRatio = Double.parseDouble(parts[2]);
        } catch (Exception ignore) { cfg.fontSizeRatio = 0.9; }

        cfg.buttons = new ArrayList<ButtonDef>();
        String csv = parts[3];
        if (!csv.isEmpty()) {
            String[] items = csv.split(",");
            for (String item : items) {
                int p = item.indexOf(':');
                if (p <= 0) continue;
                String id = unesc(item.substring(0, p));
                String icon = unesc(item.substring(p + 1));
                cfg.buttons.add(new ButtonDef(id, icon));
            }
        }
        return cfg;
    }

    private static String esc(String s) {
        if (s == null) return "";
        // Reihenfolge wichtig
        return s.replace("\\", "\\\\").replace("|", "\\p").replace(",", "\\c").replace(":", "\\d");
    }

    private static String unesc(String s) {
        if (s == null) return "";
        // Reihenfolge beachten: umgekehrte Maskierung
        return s.replace("\\p", "|").replace("\\c", ",").replace("\\d", ":").replace("\\\\", "\\");
    }
}
