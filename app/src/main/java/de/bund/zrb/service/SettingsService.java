package de.bund.zrb.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Manages settings as JSON files in the user's .wd4j folder.
 */
public class SettingsService {

    private static final String APP_FOLDER = ".wd4j";
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

    public void save(String fileName, Object data) {
        Path file = basePath.resolve(fileName);
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save settings: " + file, e);
        }
    }
}
