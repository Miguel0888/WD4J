package wd4j.impl.webdriver.type.browsingContext.parameters;

public class PrintMarginParameters {
    private final float bottom;
    private final float left;
    private final float right;
    private final float top;

    public PrintMarginParameters(float bottom, float left, float right, float top) {
        this.bottom = bottom;
        this.left = left;
        this.right = right;
        this.top = top;
    }

    public float getBottom() {
        return bottom;
    }

    public float getLeft() {
        return left;
    }

    public float getRight() {
        return right;
    }

    public float getTop() {
        return top;
    }
}
