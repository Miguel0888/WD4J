package de.bund.zrb.ui;

import de.bund.zrb.ui.commandframework.MenuCommand;
import de.bund.zrb.ui.commandframework.ToolbarButtonConfig;
import de.bund.zrb.ui.commandframework.ToolbarConfig;

import java.util.*;

public final class ToolbarDefaults {
    private ToolbarDefaults() {}

    /** Erstkonfiguration: wichtige Buttons sichtbar, alle anderen als hidden markiert. */
    public static ToolbarConfig createInitialConfig(List<MenuCommand> allCommands) {
        ToolbarConfig cfg = new ToolbarConfig();
        cfg.buttonSizePx = 48;
        cfg.fontSizeRatio = 0.75f;
        cfg.groupColors = new LinkedHashMap<>();
        cfg.rightSideIds = new LinkedHashSet<>();

        List<ToolbarButtonConfig> important = buildImportantDefaultButtons(allCommands);
        cfg.buttons = new ArrayList<>(important);

        // Hidden = alle übrigen
        cfg.hiddenCommandIds = new LinkedHashSet<>();
        Set<String> visibleIds = new LinkedHashSet<>();
        for (ToolbarButtonConfig b : important) visibleIds.add(b.id);
        for (MenuCommand mc : allCommands) {
            String id = Objects.toString(mc.getId(), "");
            if (!visibleIds.contains(id)) cfg.hiddenCommandIds.add(id);
        }
        return cfg;
    }

    /** „Wichtige“ Standard-Buttons inkl. Filmklappe 🎬 (video.toggle). */
    public static List<ToolbarButtonConfig> buildImportantDefaultButtons(List<MenuCommand> allCommands) {
        List<ToolbarButtonConfig> visible = new ArrayList<>();

        String idPlay        = findIdContaining(allCommands,"testsuite.play","playtestsuite","playtest","play");
        String idStopPlay    = findIdContaining(allCommands,"stopplayback","stop.playback","stopplay");
        String idStartRec    = findIdContaining(allCommands,"startrecord","record.start");
        String idStopRec     = findIdContaining(allCommands,"stoprecord","record.stop");
        String idLogin       = findIdContaining(allCommands,"loginuser","login");
        String idHome        = findIdContaining(allCommands,"navigation.home","home");
        String idOtp         = findIdContaining(allCommands,"showotpdialog","otp");
        String idCloseTab    = findIdContaining(allCommands,"closetab","close.tab");
        String idReload      = findIdContaining(allCommands,"reloadtab","reload","refresh");
        String idUserReg     = findIdContaining(allCommands,"userregistry","user.registry","credentials","zugangsdaten");
        String idVideoToggle = findIdContaining(allCommands,"video.toggle","recording.toggle","video.record");
        // NEU: Seitenleisten-Toggles
        String idToggleLeft  = findIdContaining(allCommands, "view.toggleleftdrawer", "view.toggleleft");
        String idToggleRight = findIdContaining(allCommands, "view.togglerightdrawer", "view.toggleright");

        class Def { String icon; String hex; Def(String i,String h){icon=i;hex=h;} }
        Map<String, Def> m = new LinkedHashMap<>();
        m.put(idPlay,        new Def("▶", "#00AA00"));
        m.put(idStopPlay,    new Def("■", "#00AA00"));
        m.put(idStartRec,    new Def("⦿", "#FF0000"));
        m.put(idStopRec,     new Def("■", "#FF0000"));
        m.put(idLogin,       new Def(cp(0x1F511), "#FFD700")); // 🔑
        m.put(idHome,        new Def(cp(0x1F3E0), "#FFD700")); // 🏠
        m.put(idOtp,         new Def("🔢", "#FFD700"));
        m.put(idCloseTab,    new Def("✖", null));
        m.put(idReload,      new Def("↻", null));
        m.put(idUserReg,     new Def(cp(0x1F4C7), null));      // 📇
        m.put(idVideoToggle, new Def(cp(0x1F3AC), null));      // 🎬
        // NEU: Icons für Seitenleisten
        m.put(idToggleLeft,  new Def("⟨", null));
        m.put(idToggleRight, new Def("⟩", null));

        String[] order = new String[]{
                idPlay, idStopPlay, idStartRec, idStopRec, idVideoToggle,
                idLogin, idHome, idOtp, idCloseTab, idReload, idUserReg,
                // NEU: nach den Standard-Navigationsaktionen einreihen
                idToggleLeft, idToggleRight
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

    /** Fallback-Icon, wenn ein Command ohne manuell gesetztes Icon auftaucht. */
    public static String defaultIconFor(String idRaw) {
        String id = idRaw == null ? "" : idRaw.toLowerCase(Locale.ROOT);
        if (id.contains("record.play"))      return "▶";
        if (id.contains("record.stop"))      return "■";
        if (id.contains("record.toggle"))    return "⦿";
        if (id.contains("testsuite.play"))   return "▶";
        if (id.contains("testsuite.stop"))   return "■";
        if (id.contains("browser.launch") || id.contains("launch")) return cp(0x1F310); // 🌐
        if (id.contains("terminate"))                                return "■";
        if (id.contains("newtab"))                                   return "＋";
        if (id.contains("closetab") || id.contains("close"))         return "✖";
        if (id.contains("reload") || id.contains("refresh"))         return "↻";
        if (id.contains("back"))                                     return "←";
        if (id.contains("forward"))                                  return "→";
        if (id.contains("home"))                                     return cp(0x1F3E0); // 🏠
        if (id.contains("screenshot") || id.contains("capture"))     return cp(0x1F4F7); // 📷
        if (id.contains("selectors"))                                return cp(0x1F50D); // 🔍
        if (id.contains("domevents"))                                return cp(0x1F4DC); // 📜
        if (id.contains("userselection") || id.contains("userregistry")) return cp(0x1F4C7); // 📇
        if (id.contains("login") || id.contains("otp"))                   return cp(0x1F511); // 🔑
        if (id.contains("view.toggleleft"))                           return "⟨";
        if (id.contains("view.toggleright"))                          return "⟩";
        if (id.contains("settings") || id.contains("configure"))      return "⚙";
        if (id.contains("shortcut"))                                  return "⌘";
        if (id.contains("video.toggle") || id.contains("video.record")) return cp(0x1F3AC);   // 🎬
        return "●";
    }

    /** Fallback-Farbe (nur wenn kein Button- oder Gruppen-HEX gesetzt ist). */
    public static String defaultBackgroundHexFor(String idRaw) {
        String id = idRaw == null ? "" : idRaw.toLowerCase(Locale.ROOT);
        if (id.contains("record")) return "#FF0000";
        if (id.contains("testsuite.play") || id.contains("play")) return "#00AA00";
        return null;
    }

    private static String findIdContaining(List<MenuCommand> all, String... tokens) {
        for (String t : tokens) {
            String needle = t.toLowerCase(Locale.ROOT);
            for (MenuCommand mc : all) {
                String id = Objects.toString(mc.getId(), "");
                if (id.toLowerCase(Locale.ROOT).contains(needle)) return id;
            }
        }
        return null;
    }

    private static String cp(int codePoint) {
        return new String(Character.toChars(codePoint));
    }
}
