package de.bund.zrb.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Manages all WD4J JSON config files (settings, shortcuts, tests).
 */
public class SettingsService {

    private static final String APP_FOLDER = ".wd4j";
    private static final String SHORTCUT_FILE_NAME = "shortcut.json";
    private static final String TESTS_FILE_NAME = "tests.json";

    private static SettingsService instance;
    private final Gson gson;
    private final Path basePath;

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
    }

    public static synchronized SettingsService getInstance() {
        if (instance == null) {
            instance = new SettingsService();
        }
        return instance;
    }

    /** Generic load for any file */
    public <T> T load(String fileName, Class<T> type) {
        Path file = basePath.resolve(fileName);
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                return gson.fromJson(reader, type);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load settings: " + file, e);
            }
        } else {
            return null;
        }
    }

    /** Generic save for any file */
    public void save(String fileName, Object data) {
        Path file = basePath.resolve(fileName);
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save settings: " + file, e);
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
}
