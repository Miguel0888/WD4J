package wd4j.impl.webdriver.type.input.parameters.sourceActions;

import wd4j.impl.webdriver.type.input.parameters.SourceActions;

import java.util.List;

public class WheelSourceActions extends SourceActions {
    private final String id;
    private final List<WheelSourceAction> actions;

    public WheelSourceActions(String id, List<WheelSourceAction> actions) {
        super("wheel");
        this.id = id;
        this.actions = actions;
    }

    public String getId() {
        return id;
    }

    public List<WheelSourceAction> getActions() {
        return actions;
    }
}
