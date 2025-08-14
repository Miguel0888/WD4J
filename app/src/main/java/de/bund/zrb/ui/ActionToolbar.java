package de.bund.zrb.ui;

import de.bund.zrb.service.SettingsService;
import de.bund.zrb.ui.commandframework.CommandRegistryImpl;
import de.bund.zrb.ui.commandframework.ToolbarConfig;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Toolbar mit konfigurierbaren Buttons und Persistenz über SettingsService.
 * Speichert/liest die Konfiguration als String unter dem Key "toolbarConfig".
 */
public class ActionToolbar extends JToolBar {
    private static final String SETTINGS_KEY = "toolbarConfig";

    private static ActionToolbar instance;
    private final Map<String, JButton> buttons = new HashMap<>();
    private final CommandRegistryImpl commandRegistry;
    private ToolbarConfig currentConfig;

    public ActionToolbar(CommandRegistryImpl registry) {
        this.commandRegistry = registry;
        instance = this;
        setFloatable(false);
        loadToolbarSettings();
    }

    /** Singleton-Getter, nützlich für z.B. Reset aus SettingsCommand. */
    public static ActionToolbar getInstance() { return instance; }

    /** Setzt auf Standard zurück: Alle Befehle, Unicode-Icons, speichert und lädt neu. */
    public static void resetToDefault() {
        if (instance != null) {
            instance.currentConfig = createDefaultConfig();
            instance.saveToolbarSettings();
            instance.reload();
        }
    }

    /** Toolbar gemäß currentConfig neu aufbauen. */
    private void reload() {
        removeAll();
        buttons.clear();

        if (currentConfig == null || currentConfig.getButtons() == null) {
            revalidate(); repaint();
            return;
        }

        int btnSize = Math.max(24, currentConfig.getButtonSizePx());
        float fontRatio = (float) Math.max(0.5, Math.min(2.0, currentConfig.getFontSizeRatio()));

        for (ToolbarConfig.ButtonConfig bc : currentConfig.getButtons()) {
            commandRegistry.getById(bc.id).ifPresent(cmd -> {
                JButton b = new JButton(bc.icon);
                b.setFocusPainted(false);
                b.setToolTipText(cmd.getLabel());
                b.addActionListener(ev -> cmd.perform());
                b.setPreferredSize(new Dimension(btnSize, btnSize));
                Font base = b.getFont();
                b.setFont(base.deriveFont(base.getSize2D() * fontRatio));
                add(b);
                buttons.put(bc.id, b);
            });
        }

        revalidate();
        repaint();
    }

    /* ======================================================================================
       Settings: Laden/Speichern
       ====================================================================================== */

    private void loadToolbarSettings() {
        try {
            String raw = SettingsService.getInstance().get(SETTINGS_KEY, String.class);
            if (raw == null || raw.trim().isEmpty()) {
                currentConfig = createDefaultConfig();
            } else {
                currentConfig = deserialize(raw);
                if (currentConfig == null || currentConfig.getButtons() == null || currentConfig.getButtons().isEmpty()) {
                    currentConfig = createDefaultConfig();
                }
            }
        } catch (Exception ex) {
            // Fallback bei Problemen
            currentConfig = createDefaultConfig();
        }
        reload();
    }

    private void saveToolbarSettings() {
        try {
            String raw = serialize(currentConfig);
            SettingsService.getInstance().set(SETTINGS_KEY, raw);
        } catch (Exception ignored) {
            // Best effort — kein harter Fehler
        }
    }

    /* ======================================================================================
       Default-Konfiguration
       ====================================================================================== */

    /** Erzeugt Standardkonfiguration mit allen registrierten Commands und sinnvollen Unicode-Icons. */
    private static ToolbarConfig createDefaultConfig() {
        List<ToolbarConfig.ButtonConfig> btns = new ArrayList<>();

        // Id → Icon (Unicode) – hier die wichtigsten Icons vorbelegen
        Map<String, String> preferredIcons = new HashMap<>();
        preferredIcons.put("testsuite.play", "▶");
        preferredIcons.put("testsuite.stop", "■");
        preferredIcons.put("browser.launch", "🌐");
        preferredIcons.put("view.toggleLeftDrawer", "⟨");
        preferredIcons.put("view.toggleRightDrawer", "⟩");
        preferredIcons.put("file.configure", "⚙");

        CommandRegistryImpl registry = CommandRegistryImpl.getInstance();
        for (String id : safeAllCommandIds(registry)) {
            // Für die Toolbar erstmal alles aufnehmen; Unbekanntes bekommt ein Fallback-Icon
            String icon = preferredIcons.getOrDefault(id, "⬚");
            btns.add(new ToolbarConfig.ButtonConfig(id, icon));
        }

        ToolbarConfig cfg = new ToolbarConfig();
        cfg.setButtons(btns);
        cfg.setButtonSizePx(32);
        cfg.setFontSizeRatio(0.90);
        return cfg;
    }

    /**
     * Versucht alle Command-IDs aus dem Registry zu holen.
     * 1) Methode getAllCommandIds() (falls vorhanden)
     * 2) Fallback: Methode getAll() und via Reflection getId() extrahieren
     */
    @SuppressWarnings("unchecked")
    private static List<String> safeAllCommandIds(CommandRegistryImpl registry) {
        try {
            // Versuch 1: getAllCommandIds()
            try {
                var m = registry.getClass().getMethod("getAllCommandIds");
                Object res = m.invoke(registry);
                if (res instanceof Collection) {
                    List<String> out = new ArrayList<>();
                    for (Object o : (Collection<?>) res) {
                        if (o != null) out.add(String.valueOf(o));
                    }
                    return out;
                }
            } catch (NoSuchMethodException ignore) { /* weiter mit Fallback */ }

            // Versuch 2: getAll() -> Collection<MenuCommand>
            try {
                var m = registry.getClass().getMethod("getAll");
                Object res = m.invoke(registry);
                if (res instanceof Collection) {
                    List<String> out = new ArrayList<>();
                    for (Object cmd : (Collection<?>) res) {
                        try {
                            var mid = cmd.getClass().getMethod("getId");
                            Object id = mid.invoke(cmd);
                            if (id != null) out.add(String.valueOf(id));
                        } catch (Throwable ignoreEach) { /* weiter */ }
                    }
                    // Stabil sortieren
                    Collections.sort(out);
                    return out;
                }
            } catch (NoSuchMethodException ignore) { /* kein Fallback vorhanden */ }
        } catch (Throwable t) {
            // ignore
        }
        // Minimal-Fallback: nur häufige IDs
        return Arrays.asList(
                "testsuite.play", "testsuite.stop",
                "browser.launch",
                "view.toggleLeftDrawer", "view.toggleRightDrawer",
                "file.configure"
        );
    }

    /* ======================================================================================
       Minimal-Serialisierung (String) – robust & libfrei
       Format v1:
         v1|<buttonSize>|<fontRatio>|<id1>:<icon1>,<id2>:<icon2>,...
       ====================================================================================== */

    private static String serialize(ToolbarConfig cfg) {
        if (cfg == null) return "v1|32|0.9|";
        StringBuilder sb = new StringBuilder();
        sb.append("v1|").append(cfg.getButtonSizePx())
                .append("|").append(cfg.getFontSizeRatio())
                .append("|");

        boolean first = true;
        if (cfg.getButtons() != null) {
            for (ToolbarConfig.ButtonConfig bc : cfg.getButtons()) {
                if (!first) sb.append(',');
                first = false;
                sb.append(escape(bc.id)).append(':').append(escape(bc.icon == null ? "" : bc.icon));
            }
        }
        return sb.toString();
    }

    private static ToolbarConfig deserialize(String raw) {
        try {
            if (raw == null) return null;
            String s = raw.trim();
            if (!s.startsWith("v1|")) return null;
            String[] parts = s.split("\\|", 4);
            if (parts.length < 4) return null;

            int size = Integer.parseInt(parts[1]);
            double ratio = Double.parseDouble(parts[2]);
            String list = parts[3];

            List<ToolbarConfig.ButtonConfig> btns = new ArrayList<>();
            if (!list.isEmpty()) {
                String[] items = list.split(",");
                for (String item : items) {
                    int p = item.indexOf(':');
                    if (p <= 0) continue;
                    String id = unescape(item.substring(0, p));
                    String icon = unescape(item.substring(p + 1));
                    btns.add(new ToolbarConfig.ButtonConfig(id, icon));
                }
            }

            ToolbarConfig cfg = new ToolbarConfig();
            cfg.setButtons(btns);
            cfg.setButtonSizePx(size);
            cfg.setFontSizeRatio(ratio);
            return cfg;
        } catch (Exception ex) {
            return null;
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("|", "\\p").replace(",", "\\c").replace(":", "\\d");
    }

    private static String unescape(String s) {
        if (s == null) return "";
        // Reihenfolge beachten: erst Backslash
        return s.replace("\\p", "|").replace("\\c", ",").replace("\\d", ":").replace("\\\\", "\\");
    }
}
