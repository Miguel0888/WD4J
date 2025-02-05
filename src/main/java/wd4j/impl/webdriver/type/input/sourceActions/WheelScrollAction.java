package wd4j.impl.webdriver.type.input.sourceActions;

import wd4j.impl.webdriver.type.input.Origin;
import wd4j.impl.webdriver.type.input.SourceActions;

public class WheelScrollAction extends SourceActions implements WheelSourceAction {
    private final int x;
    private final int y;
    private final int deltaX;
    private final int deltaY;
    private long duration;
    private Origin origin; // default is Origin. "viewport"

    public WheelScrollAction(int x, int y, int deltaX, int deltaY) {
        super("scroll");
        this.x = x;
        this.y = y;
        this.deltaX = deltaX;
        this.deltaY = deltaY;
        this.duration = 0;
        this.origin = new Origin("viewport");
    }

    public WheelScrollAction(int x, int y, int deltaX, int deltaY, long duration) {
        super("scroll");
        this.x = x;
        this.y = y;
        this.deltaX = deltaX;
        this.deltaY = deltaY;
        this.duration = duration;
        this.origin = new Origin("viewport");
    }

    public WheelScrollAction(int x, int y, int deltaX, int deltaY, Origin origin) {
        super("scroll");
        this.x = x;
        this.y = y;
        this.deltaX = deltaX;
        this.deltaY = deltaY;
        this.duration = 0;
        this.origin = origin;
    }

    public WheelScrollAction(int x, int y, int deltaX, int deltaY, long duration, Origin origin) {
        super("scroll");
        this.x = x;
        this.y = y;
        this.deltaX = deltaX;
        this.deltaY = deltaY;
        this.duration = duration;
        this.origin = origin;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getDeltaX() {
        return deltaX;
    }

    public int getDeltaY() {
        return deltaY;
    }

    public long getDuration() {
        return duration;
    }

    public Origin getOrigin() {
        return origin;
    }

}
