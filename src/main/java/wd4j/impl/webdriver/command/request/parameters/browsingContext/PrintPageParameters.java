package wd4j.impl.webdriver.command.request.parameters.browsingContext;

public class PrintPageParameters {
    private final float height;
    private final float width;

    public PrintPageParameters(float height, float width) {
        this.height = height;
        this.width = width;
    }

    public float getHeight() {
        return height;
    }

    public float getWidth() {
        return width;
    }
}
