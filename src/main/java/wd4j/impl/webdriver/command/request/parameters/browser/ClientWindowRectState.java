package wd4j.impl.webdriver.command.request.parameters.browser;

public class ClientWindowRectState implements ClientWindowState{
    private final String state = "normal";
    private final char width;
    private final char height;
    private final int x;
    private final int y;

    public ClientWindowRectState(char width, char height, int x, int y) {
        this.width = width;
        this.height = height;
        this.x = x;
        this.y = y;
    }

    public String getState() {
        return state;
    }

    public char getWidth() {
        return width;
    }

    public char getHeight() {
        return height;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}
