package app.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Settings {
    private static final String SETTINGS_FILE = "settings.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Beispielhafte Einstellungen (Erweiterbar)
    private String otpSecret;   // Für die 2FA OneTimePad
    private boolean headlessMode;
    private String profilePath;

    private Settings() {
        loadSettings();
    }

    private static final class InstanceHolder {
        static final Settings instance = new Settings();
    }

    public static Settings getInstance() {
        return InstanceHolder.instance;
    }

    private void loadSettings() {
        try {
            Path path = Paths.get(SETTINGS_FILE);
            if (Files.exists(path)) {
                try (Reader reader = new FileReader(SETTINGS_FILE)) {
                    Settings loadedSettings = GSON.fromJson(reader, Settings.class);
                    if (loadedSettings != null) {
                        this.otpSecret = loadedSettings.otpSecret;
                        this.headlessMode = loadedSettings.headlessMode;
                        this.profilePath = loadedSettings.profilePath;
                    }
                }
            } else {
                saveSettings(); // Falls keine Datei existiert, mit Standardwerten speichern
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveSettings() {
        try (Writer writer = new FileWriter(SETTINGS_FILE)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Getter und Setter
    public String getOtpSecret() {
        return otpSecret;
    }

    public void setOtpSecret(String otpSecret) {
        this.otpSecret = otpSecret;
        saveSettings();
    }

    public boolean isHeadlessMode() {
        return headlessMode;
    }

    public void setHeadlessMode(boolean headlessMode) {
        this.headlessMode = headlessMode;
        saveSettings();
    }

    public String getProfilePath() {
        return profilePath;
    }

    public void setProfilePath(String profilePath) {
        this.profilePath = profilePath;
        saveSettings();
    }
}
