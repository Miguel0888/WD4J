package wd4j.impl.webdriver.type.browser;

import wd4j.impl.markerInterfaces.WDType;
import wd4j.impl.webdriver.mapping.EnumWrapper;

public class WDClientWindowInfo implements WDType<WDClientWindowInfo> {
    private final boolean active;
    private final WDClientWindow clientWindow;
    private final char height;
    private final State state;
    private final char width;
    private final int x;
    private final int y;

    public WDClientWindowInfo(boolean active, WDClientWindow clientWindow, char height, State state, char width, int x, int y) {
        this.active = active;
        this.clientWindow = clientWindow;
        this.height = height;
        this.state = state;
        this.width = width;
        this.x = x;
        this.y = y;
    }

    public boolean isActive() {
        return active;
    }

    public WDClientWindow getClientWindow() {
        return clientWindow;
    }

    public char getHeight() {
        return height;
    }

    public State getState() {
        return state;
    }

    public char getWidth() {
        return width;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    // ToDo: See similar implementation in ClientWindowNamedState.java & ClientWindowRectState.java
    public enum State implements EnumWrapper {
        FULLSCREEN ("fullscreen"),
        MAXIMIZED ("maximized"),
        MINIMIZED ("minimized"),
        NORMAL ("normal");

        private final String value;

        State(String value) {
            this.value = value;
        }

        @Override // confirmed
        public String value() {
            return value;
        }
    }
}