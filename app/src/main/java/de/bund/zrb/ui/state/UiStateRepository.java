package de.bund.zrb.ui.state;

/**
 * Port for persisting and loading UiState.
 * Implement this with filesystem, DB, etc.
 */
public interface UiStateRepository {

    /**
     * Load last persisted UiState or return defaults if none found.
     */
    UiState loadUiState();

    /**
     * Persist given UiState atomically.
     */
    void saveUiState(UiState state);
}
