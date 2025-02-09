package wd4j.impl.webdriver.command.request.browsingContext.parameters;

public class BoxClipRectangle implements ClipRectangle {
    private final String type = "box";
    private final int x;
    private final int y;
    private final int width;
    private final int height;

    public BoxClipRectangle(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public String getType() {
        return type;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
