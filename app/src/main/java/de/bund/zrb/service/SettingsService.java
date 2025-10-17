package de.bund.zrb.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages all WD4J JSON config files (settings, shortcuts, tests).
 */
public class SettingsService {

    private static final String APP_FOLDER = ".wd4j";
    private static final String SHORTCUT_FILE_NAME = "shortcut.json";
    private static final String TESTS_FILE_NAME = "tests.json";
    private static final String SETTINGS_FILE_NAME = "settings.json";
    private static final String REGEX_FILE_NAME = "regex.json";

    private static SettingsService instance;

    private final Gson gson;
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
