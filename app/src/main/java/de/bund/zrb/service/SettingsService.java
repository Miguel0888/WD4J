package de.bund.zrb.service;

import de.bund.zrb.config.SettingsData;

public interface SettingsService {
    void saveSettings(SettingsData data);
    SettingsData loadSettings();
}
