package de.bund.zrb.ui.state;

/**
 * Represent persistent window/UI layout state between application runs.
 * Keep this class as a dumb data holder (POJO), no logic.
 */
public class UiState {

    private WindowState mainWindow;
    private DrawerState leftDrawer;
    private DrawerState rightDrawer;

    public UiState() {
        // Initialize with safe defaults
        this.mainWindow = new WindowState();
        this.leftDrawer = new DrawerState();
        this.rightDrawer = new DrawerState();
    }

    public WindowState getMainWindow() {
        return mainWindow;
    }

    public void setMainWindow(WindowState mainWindow) {
        this.mainWindow = mainWindow;
    }

    public DrawerState getLeftDrawer() {
        return leftDrawer;
    }

    public void setLeftDrawer(DrawerState leftDrawer) {
        this.leftDrawer = leftDrawer;
    }

    public DrawerState getRightDrawer() {
        return rightDrawer;
    }

    public void setRightDrawer(DrawerState rightDrawer) {
        this.rightDrawer = rightDrawer;
    }

    /**
     * Nested class that captures the primary window geometry.
     */
    public static class WindowState {
        private int x;
        private int y;
        private int width;
        private int height;
        private boolean maximized;

        public WindowState() {
            // Provide conservative defaults
            this.x = 100;
            this.y = 100;
            this.width = 1280;
            this.height = 800;
            this.maximized = false;
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public boolean isMaximized() {
            return maximized;
        }

        public void setMaximized(boolean maximized) {
            this.maximized = maximized;
        }
    }

    /**
     * Nested class that captures sidebar / drawer layout.
     */
    public static class DrawerState {
        private boolean visible;
        private int width;

        public DrawerState() {
            // Give defaults that won't break layout
            this.visible = true;
            this.width = 300;
        }

        public boolean isVisible() {
            return visible;
        }

        public void setVisible(boolean visible) {
            this.visible = visible;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }
    }
}
