package de.bund.zrb.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SettingsData {
    private static final String SETTINGS_FILE = "settings.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static SettingsData instance = null;

    // 🔹 Deine vorhandenen Variablen bleiben unverändert!
    private String otpSecret;   // Für die 2FA OneTimePad
    private String port = "9222";
    private boolean useProfile = true;
    private String profilePath = "";
    private boolean headlessMode = false;
    private boolean disableGpu = false;
    private boolean noRemote = false;
    private boolean startMaximized = false;

    private SettingsData() {
        // ** WICHTIG: Instanz zuerst setzen, dann Settings laden! **
        instance = this;
        loadSettings();
    }

    public static synchronized SettingsData getInstance() {
        if (instance == null) {
            instance = new SettingsData();
        }
        return instance;
    }

    public void loadSettings() {
        Path path = Paths.get(SETTINGS_FILE);
        if (Files.exists(path)) {
            try (Reader reader = new FileReader(SETTINGS_FILE)) {
                // ToDo: Fix StackOverflowError
//                SettingsData loadedSettingsData = GSON.fromJson(reader, SettingsData.class);
//                if (loadedSettingsData != null) {
//                    // 🔥 ALLE Variablen korrekt zuweisen
//                    this.otpSecret = loadedSettingsData.otpSecret;
//                    this.port = loadedSettingsData.port;
//                    this.useProfile = loadedSettingsData.useProfile;
//                    this.profilePath = loadedSettingsData.profilePath;
//                    this.headlessMode = loadedSettingsData.headlessMode;
//                    this.disableGpu = loadedSettingsData.disableGpu;
//                    this.noRemote = loadedSettingsData.noRemote;
//                    this.startMaximized = loadedSettingsData.startMaximized;
//                }
            } catch (Exception e) {
                System.err.println("⚠ Fehler beim Laden der Einstellungen: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("ℹ Keine settings.json gefunden. Standardwerte werden verwendet.");
            // WICHTIG: NICHT direkt speichern, sondern erst sicherstellen, dass alles korrekt läuft!
        }
    }

    public void saveSettings() {
        if (instance == null) {
            System.err.println("⚠ Fehler: Instanz von SettingsData ist null!");
            return;
        }
        try (Writer writer = new FileWriter(SETTINGS_FILE)) {
            GSON.toJson(instance, writer);
        } catch (IOException e) {
            System.err.println("⚠ Fehler beim Speichern der Einstellungen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 🔹 Getter und Setter (alle wie vorher, kein Verlust!)

    public String getOtpSecret() {
        return otpSecret;
    }

    public void setOtpSecret(String otpSecret) {
        this.otpSecret = otpSecret;
        saveSettings();
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
        saveSettings();
    }

    public boolean isUseProfile() {
        return useProfile;
    }

    public void setUseProfile(boolean useProfile) {
        this.useProfile = useProfile;
        saveSettings();
    }

    public String getProfilePath() {
        return profilePath;
    }

    public void setProfilePath(String profilePath) {
        this.profilePath = profilePath;
        saveSettings();
    }

    public boolean isHeadlessMode() {
        return headlessMode;
    }

    public void setHeadlessMode(boolean headlessMode) {
        this.headlessMode = headlessMode;
        saveSettings();
    }

    public boolean isDisableGpu() {
        return disableGpu;
    }

    public void setDisableGpu(boolean disableGpu) {
        this.disableGpu = disableGpu;
        saveSettings();
    }

    public boolean isNoRemote() {
        return noRemote;
    }

    public void setNoRemote(boolean noRemote) {
        this.noRemote = noRemote;
        saveSettings();
    }

    public boolean isStartMaximized() {
        return startMaximized;
    }

    public void setStartMaximized(boolean startMaximized) {
        this.startMaximized = startMaximized;
        saveSettings();
    }
}
