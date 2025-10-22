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
