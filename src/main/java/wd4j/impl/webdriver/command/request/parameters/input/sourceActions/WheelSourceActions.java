package wd4j.impl.webdriver.command.request.parameters.input.sourceActions;

import wd4j.impl.webdriver.command.request.parameters.input.PerformActionsParameters;

import java.util.List;

public class WheelSourceActions extends PerformActionsParameters.SourceActions {
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
