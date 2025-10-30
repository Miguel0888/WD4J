package de.bund.zrb.ui.state;

/**
 * Application service to manage persistent UI state.
 * Use this from UI layer (e.g. when opening/closing the main window).
 */
public class UiStateService {

    private final UiStateRepository repository;
    private UiState cachedState;

    public UiStateService(UiStateRepository repository) {
        this.repository = repository;
        this.cachedState = repository.loadUiState();
    }

    /**
     * Return current in-memory UiState snapshot.
     * UI code can read values from here on startup.
     */
    public UiState getUiState() {
        return cachedState;
    }

    /**
     * Update the window metrics (geometry) in memory.
     * Do not persist immediately; let caller decide when to persist.
     */
    public void updateMainWindowState(int x, int y, int w, int h, boolean maximized) {
        UiState.WindowState win = cachedState.getMainWindow();
        win.setX(x);
        win.setY(y);
        win.setWidth(w);
        win.setHeight(h);
        win.setMaximized(maximized);
    }

    /**
     * Update the left drawer information.
     */
    public void updateLeftDrawerState(boolean visible, int width) {
        UiState.DrawerState drawer = cachedState.getLeftDrawer();
        drawer.setVisible(visible);
        drawer.setWidth(width);
    }

    /**
     * Update the right drawer information.
     */
    public void updateRightDrawerState(boolean visible, int width) {
        UiState.DrawerState drawer = cachedState.getRightDrawer();
        drawer.setVisible(visible);
        drawer.setWidth(width);
    }

    /**
     * Persist the currently cached UiState to disk.
     * Call this e.g. on windowClosing.
     */
    public void persist() {
        repository.saveUiState(cachedState);
    }
}
