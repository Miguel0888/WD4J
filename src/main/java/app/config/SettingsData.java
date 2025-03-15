package app.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SettingsData {
    private static final String SETTINGS_FILE = "settings.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private String otpSecret;   // Für die 2FA OneTimePad
    private String port = "9222";
    private boolean useProfile = true;
    private String profilePath = "";
    private boolean headlessMode = false;
    private boolean disableGpu = false;
    private boolean noRemote = false;
    private boolean startMaximized = false;

    private SettingsData() {
        loadSettings();
    }

    private static final class InstanceHolder {
        static final SettingsData instance = new SettingsData();
    }

    public static SettingsData getInstance() {
        return InstanceHolder.instance;
    }

    private void loadSettings() {
        try {
            Path path = Paths.get(SETTINGS_FILE);
            if (Files.exists(path)) {
                try (Reader reader = new FileReader(SETTINGS_FILE)) {
                    SettingsData loadedSettingsData = GSON.fromJson(reader, SettingsData.class);
                    if (loadedSettingsData != null) {
                        this.otpSecret = loadedSettingsData.otpSecret;
                        this.port = loadedSettingsData.port;
                        this.useProfile = loadedSettingsData.useProfile;
                        this.profilePath = loadedSettingsData.profilePath;
                        this.headlessMode = loadedSettingsData.headlessMode;
                        this.disableGpu = loadedSettingsData.disableGpu;
                        this.noRemote = loadedSettingsData.noRemote;
                        this.startMaximized = loadedSettingsData.startMaximized;
                    }
                }
            } else {
                saveSettings(); // Falls keine Datei existiert, wird mit den Default-Werten gespeichert
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
