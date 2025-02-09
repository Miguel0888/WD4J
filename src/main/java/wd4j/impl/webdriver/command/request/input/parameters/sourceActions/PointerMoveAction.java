package wd4j.impl.webdriver.command.request.input.parameters.sourceActions;

public class PointerMoveAction extends PointerCommonProperties implements PointerSourceAction {
    private final String type = "pointerMove"; // ToDo: This is weird.
    private final int x;
    private final int y;
    private final int duration;
    private final Origin origin;

    public PointerMoveAction(int x, int y, int duration, Origin origin, PointerCommonProperties commonProperties) {
        this.x = x;
        this.y = y;
        this.duration = duration;
        this.origin = origin;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getDuration() {
        return duration;
    }
}
