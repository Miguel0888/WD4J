package de.bund.zrb.ui.state;

import de.bund.zrb.service.SettingsService;

/**
 * File-based implementation of UiStateRepository using SettingsService.
 * This is the infrastructure adapter.
 */
public class FileUiStateRepository implements UiStateRepository {

    // Reuse your proposed filename
    private static final String UI_FILE_NAME = "ui.json";

    private final SettingsService settingsService;

    public FileUiStateRepository() {
        // Depend on abstraction of SettingsService as singleton for now.
        // If du willst noch weniger Kopplung: inject SettingsService als Interface.
        this.settingsService = SettingsService.getInstance();
    }

    public FileUiStateRepository(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @Override
    public UiState loadUiState() {
        UiState loaded = settingsService.load(UI_FILE_NAME, UiState.class);
        if (loaded == null) {
            // Return sane defaults when file does not exist yet
            return new UiState();
        }
        return loaded;
    }

    @Override
    public void saveUiState(UiState state) {
        if (state == null) {
            return;
        }
        settingsService.save(UI_FILE_NAME, state);
    }
}
