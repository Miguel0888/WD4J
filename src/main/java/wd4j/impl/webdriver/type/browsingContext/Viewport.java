package wd4j.impl.webdriver.type.browsingContext;

public class Viewport {
    private final int width;
    private final int height;

    public Viewport(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
