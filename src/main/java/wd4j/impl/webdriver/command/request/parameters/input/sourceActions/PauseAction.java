package wd4j.impl.webdriver.command.request.parameters.input.sourceActions;

public class PauseAction extends KeySourceActions implements NoneSourceAction, WheelSourceAction, PointerSourceAction {
    private final long duration;

    public PauseAction(long duration) {
        super("pause");
        this.duration = duration;
    }

    public long getDuration() {
        return duration;
    }
}