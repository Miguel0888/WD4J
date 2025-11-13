package de.bund.zrb.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.bund.zrb.config.InputDelaysConfig;
import de.bund.zrb.config.VideoConfig;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages all WD4J JSON config files (settings, shortcuts, tests).
 */
public class SettingsService {

    public static final String APP_FOLDER = ".wd4j";
    private static final String SHORTCUT_FILE_NAME = "shortcut.json";
    private static final String TESTS_FILE_NAME = "tests.json";
    private static final String SETTINGS_FILE_NAME = "settings.json";
    private static final String REGEX_FILE_NAME = "regex.json";

    private static SettingsService instance;

    private final Gson gson;

    public Path getBasePath() {
        return basePath;
    }

    private final Path basePath;

    private Map<String, Object> settingsCache;

    private SettingsService() {
        gson = new GsonBuilder().setPrettyPrinting().create();
        basePath = Paths.get(System.getProperty("user.home"), APP_FOLDER);

        if (!Files.exists(basePath)) {
            try {
                Files.createDirectories(basePath);
            } catch (IOException e) {
                throw new RuntimeException("Could not create settings folder: " + basePath, e);
            }
        }

        loadSettings();
    }

    public static synchronized SettingsService getInstance() {
        if (instance == null) {
            instance = new SettingsService();
        }
        return instance;
    }

    public static synchronized void initAdapter() {
        Integer kd = getInstance().get("input.keyDownDelayMs", Integer.class);
        Integer ku = getInstance().get("input.keyUpDelayMs",   Integer.class);
        if (kd != null) InputDelaysConfig.setKeyDownDelayMs(kd);
        if (ku != null) InputDelaysConfig.setKeyUpDelayMs(ku);

        Boolean vEnabled = getInstance().get("video.enabled", Boolean.class);
        Integer vFps     = getInstance().get("video.fps",     Integer.class);
        String  vDir     = getInstance().get("video.reportsDir", String.class); // <- NEU

        if (vEnabled != null) VideoConfig.setEnabled(vEnabled);
        if (vFps != null)     VideoConfig.setFps(vFps);
        if (vDir != null && !vDir.trim().isEmpty()) VideoConfig.setReportsDir(vDir); // <- NEU

        // Advanced Video Settings:
        // in SettingsService.initAdapter() — NACH den drei bestehenden Video-Basics:
        String vContainer = getInstance().get("video.container", String.class);
        String vCodec     = getInstance().get("video.codec", String.class);
        String vPixFmt    = getInstance().get("video.pixfmt", String.class);
        Boolean vInter    = getInstance().get("video.interleaved", Boolean.class);
        String vQuality   = getInstance().get("video.quality", String.class);
        Integer vQscale   = getInstance().get("video.qscale", Integer.class);
        Integer vCrf      = getInstance().get("video.crf", Integer.class);
        Integer vBr       = getInstance().get("video.bitrateKbps", Integer.class);
        String cRange     = getInstance().get("video.color.range", String.class);
        String cSpace     = getInstance().get("video.color.space", String.class);
        String cTrc       = getInstance().get("video.color.trc", String.class);
        String cPrim      = getInstance().get("video.color.primaries", String.class);
        String vVf        = getInstance().get("video.vf", String.class);
        Integer vThreads  = getInstance().get("video.threads", Integer.class);
        Boolean vEven     = getInstance().get("video.enforceEvenDims", Boolean.class);
        List<String> vFBs = getInstance().get("video.container.fallbacks", List.class);
        String vPreset    = getInstance().get("video.preset", String.class);
        String vTune      = getInstance().get("video.tune", String.class);
        String vProfile   = getInstance().get("video.profile", String.class);
        String vLevel     = getInstance().get("video.level", String.class);
        Map<String,String> vExtra = getInstance().get("video.ffopts", Map.class);

        if (vContainer != null) VideoConfig.setContainer(vContainer);
        if (vCodec     != null) VideoConfig.setCodec(vCodec);
        if (vPixFmt    != null) VideoConfig.setPixelFmt(vPixFmt);
        if (vInter     != null) VideoConfig.setInterleaved(vInter);
        if (vQuality   != null) VideoConfig.setQualityMode(vQuality);
        if (vQscale    != null) VideoConfig.setQscale(vQscale);
        if (vCrf       != null) VideoConfig.setCrf(vCrf);
        if (vBr        != null) VideoConfig.setBitrateKbps(vBr);
        if (cRange     != null) VideoConfig.setColorRange(cRange);
        if (cSpace     != null) VideoConfig.setColorspace(cSpace);
        if (cTrc       != null) VideoConfig.setColorTrc(cTrc);
        if (cPrim      != null) VideoConfig.setColorPrimaries(cPrim);
        if (vVf        != null) VideoConfig.setVf(vVf);
        if (vThreads   != null) VideoConfig.setThreads(vThreads);
        if (vEven      != null) VideoConfig.setEnforceEvenDims(vEven);
        if (vFBs       != null) VideoConfig.setContainerFallbacks(vFBs);
        if (vPreset    != null) VideoConfig.setPreset(vPreset);
        if (vTune      != null) VideoConfig.setTune(vTune);
        if (vProfile   != null) VideoConfig.setProfile(vProfile);
        if (vLevel     != null) VideoConfig.setLevel(vLevel);
        if (vExtra     != null) {
            VideoConfig.getExtraVideoOptions().clear();
            VideoConfig.getExtraVideoOptions().putAll(vExtra);
        }

        // Debug Flag anwenden
        Boolean dbg = getInstance().get("debug.enabled", Boolean.class);
        System.setProperty("wd4j.debug", String.valueOf(dbg != null && dbg));
        Boolean dbgWs = getInstance().get("debug.websocket", Boolean.class);
        System.setProperty("wd4j.log.websocket", String.valueOf(dbgWs != null && dbgWs));
        Boolean dbgVid = getInstance().get("debug.video", Boolean.class);
        System.setProperty("wd4j.log.video", String.valueOf(dbgVid != null && dbgVid));
        Boolean dbgBrowser = getInstance().get("debug.browser", Boolean.class);
        System.setProperty("wd4j.log.browser", String.valueOf(dbgBrowser != null && dbgBrowser));

        // Command Retry Settings -> System Properties
        Integer cmdRetryCount = getInstance().get("command.retry.maxCount", Integer.class);
        Long    cmdRetryWinMs = getInstance().get("command.retry.windowMs", Long.class);
        if (cmdRetryCount != null) {
            System.setProperty("wd4j.command.retry.maxCount", String.valueOf(cmdRetryCount));
        }
        if (cmdRetryWinMs != null) {
            System.setProperty("wd4j.command.retry.windowMs", String.valueOf(cmdRetryWinMs));
        }
        // Default Action Timeout (Persistierter Wert -> System Property)
        Integer actionDefaultTimeout = getInstance().get("action.defaultTimeoutMillis", Integer.class);
        if (actionDefaultTimeout != null && actionDefaultTimeout >= 0) {
            System.setProperty("wd4j.action.defaultTimeoutMillis", String.valueOf(actionDefaultTimeout));
        }
    }

    /** Load the global settings.json into memory. */
    private void loadSettings() {
        Path file = basePath.resolve(SETTINGS_FILE_NAME);
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                settingsCache = gson.fromJson(reader, Map.class);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load settings.json", e);
            }
        } else {
            settingsCache = new HashMap<>();
        }

        if (!settingsCache.containsKey("auth.enabled")) settingsCache.put("auth.enabled", true);
        if (!settingsCache.containsKey("auth.sessionCookie")) settingsCache.put("auth.sessionCookie", "JSESSIONID");
        if (!settingsCache.containsKey("auth.redirectStatus")) settingsCache.put("auth.redirectStatus", java.util.Arrays.asList(301,302,303,307,308));
        if (!settingsCache.containsKey("auth.loginUrlPrefixes")) settingsCache.put("auth.loginUrlPrefixes", java.util.Arrays.asList("/login", "/signin"));
        // Defaults: separate waits for Assertions
        // - assertion.groupWaitMs: global wait before the group of assertions (ms)
        // - assertion.eachWaitMs: per-assertion wait (ms)
        if (!settingsCache.containsKey("assertion.groupWaitMs")) settingsCache.put("assertion.groupWaitMs", 3000);
        if (!settingsCache.containsKey("assertion.eachWaitMs"))  settingsCache.put("assertion.eachWaitMs", 0);
        if (!settingsCache.containsKey("debug.enabled")) settingsCache.put("debug.enabled", false); // steuert [DEBUG]-Ausgaben
        if (!settingsCache.containsKey("debug.websocket")) settingsCache.put("debug.websocket", false); // [WebSocket]
        if (!settingsCache.containsKey("debug.video")) settingsCache.put("debug.video", false); // [Video]
        if (!settingsCache.containsKey("debug.browser")) settingsCache.put("debug.browser", false); // Browser Prozess / Start
        // Command Retry Defaults (0 = aus)
        if (!settingsCache.containsKey("command.retry.maxCount")) settingsCache.put("command.retry.maxCount", 0);
        if (!settingsCache.containsKey("command.retry.windowMs")) settingsCache.put("command.retry.windowMs", 0L);
        // Default Action Timeout (Fallback 30000 ms wie bisher in TestTreeController/RecorderService)
        if (!settingsCache.containsKey("action.defaultTimeoutMillis")) settingsCache.put("action.defaultTimeoutMillis", 30000);

        // --- Backend-Auswahl ---
        if (!settingsCache.containsKey("video.backend")) settingsCache.put("video.backend", "vlc"); // vlc|ffmpeg|jcodec

        // --- VLC-spezifische Defaults ---
        if (!settingsCache.containsKey("video.vlc.enabled")) settingsCache.put("video.vlc.enabled", true); // Backend-Wahl
        if (!settingsCache.containsKey("video.vlc.autodetect")) settingsCache.put("video.vlc.autodetect", false); // Autodetect standardmäßig aus
        if (!settingsCache.containsKey("video.vlc.basePath")) settingsCache.put("video.vlc.basePath", defaultVlcBasePath()); // Standard-Pfad
        if (!settingsCache.containsKey("video.vlc.log.enabled")) settingsCache.put("video.vlc.log.enabled", false);
        if (!settingsCache.containsKey("video.vlc.log.path")) settingsCache.put("video.vlc.log.path", defaultVlcLogPath());
        if (!settingsCache.containsKey("video.vlc.verbose")) settingsCache.put("video.vlc.verbose", 1); // 0..2

        // VLC Aufnahme-/Transcode-Optionen
        if (!settingsCache.containsKey("video.vlc.mux")) settingsCache.put("video.vlc.mux", "mp4");
        if (!settingsCache.containsKey("video.vlc.vcodec")) settingsCache.put("video.vlc.vcodec", "h264");
        if (!settingsCache.containsKey("video.vlc.quality")) settingsCache.put("video.vlc.quality", "crf"); // crf|bitrate
        if (!settingsCache.containsKey("video.vlc.crf")) settingsCache.put("video.vlc.crf", 23);
        if (!settingsCache.containsKey("video.vlc.bitrateKbps")) settingsCache.put("video.vlc.bitrateKbps", 4000);
        if (!settingsCache.containsKey("video.vlc.deinterlace.enabled")) settingsCache.put("video.vlc.deinterlace.enabled", false);
        if (!settingsCache.containsKey("video.vlc.deinterlace.mode")) settingsCache.put("video.vlc.deinterlace.mode", "");
        if (!settingsCache.containsKey("video.vlc.videoFilter")) settingsCache.put("video.vlc.videoFilter", ""); // z.B. "postproc"
        if (!settingsCache.containsKey("video.vlc.venc.preset")) settingsCache.put("video.vlc.venc.preset", "");
        if (!settingsCache.containsKey("video.vlc.venc.tune")) settingsCache.put("video.vlc.venc.tune", "");
        if (!settingsCache.containsKey("video.vlc.soutExtras")) settingsCache.put("video.vlc.soutExtras", "");

        // Screen-Region
        if (!settingsCache.containsKey("video.vlc.screen.fullscreen")) settingsCache.put("video.vlc.screen.fullscreen", true);
        if (!settingsCache.containsKey("video.vlc.screen.left")) settingsCache.put("video.vlc.screen.left", 0);
        if (!settingsCache.containsKey("video.vlc.screen.top")) settingsCache.put("video.vlc.screen.top", 0);
        if (!settingsCache.containsKey("video.vlc.screen.width")) settingsCache.put("video.vlc.screen.width", 0);
        if (!settingsCache.containsKey("video.vlc.screen.height")) settingsCache.put("video.vlc.screen.height", 0);
        if (!settingsCache.containsKey("video.vlc.audio.enabled")) settingsCache.put("video.vlc.audio.enabled", false);
    }

    private static String defaultVlcBasePath() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) return "C:/Program Files/VideoLAN/VLC";
        if (os.contains("mac")) return "/Applications/VLC.app/Contents/MacOS/lib";
        // Linux
        return "/usr/lib"; // häufige Systempfade; Plugin-Suche ergänzt Locator
    }

    private static String defaultVlcLogPath() {
        return Paths.get(System.getProperty("user.home"), APP_FOLDER, "vlc.log").toString();
    }

    /** Persist the global settings.json to disk. */
    private void saveSettings() {
        Path file = basePath.resolve(SETTINGS_FILE_NAME);
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            gson.toJson(settingsCache, writer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save settings.json", e);
        }
    }

    /** Get a value by key. */
    public <T> T get(String key, Class<T> type) {
        Object value = settingsCache.get(key);
        if (value == null) {
            return null;
        }
        return gson.fromJson(gson.toJson(value), type);
    }

    /** Put a value by key and persist immediately. */
    public void set(String key, Object value) {
        if (value == null) {
            settingsCache.remove(key);
        } else {
            settingsCache.put(key, value);
        }
        saveSettings();
    }

    /** Generic load for any file (legacy) */
    public <T> T load(String fileName, Type type) {
        Path file = basePath.resolve(fileName);
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                return gson.fromJson(reader, type);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load: " + file, e);
            }
        } else {
            return null;
        }
    }

    /** Generic save for any file (legacy) */
    public void save(String fileName, Object data) {
        Path file = basePath.resolve(fileName);
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save: " + file, e);
        }
    }

    /** Shorthand: load shortcuts.json */
    public <T> T loadShortcuts(Class<T> type) {
        return load(SHORTCUT_FILE_NAME, type);
    }

    public void saveShortcuts(Object data) {
        save(SHORTCUT_FILE_NAME, data);
    }

    /** Shorthand: load tests.json */
    public <T> T loadTests(Class<T> type) {
        return load(TESTS_FILE_NAME, type);
    }

    public void saveTests(Object data) {
        save(TESTS_FILE_NAME, data);
    }

    public static String getRegexFileName() {
        return REGEX_FILE_NAME;
    }
}
