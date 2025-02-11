package wd4j.impl.webdriver.command.request.parameters.input.sourceActions;


import wd4j.impl.webdriver.command.request.parameters.input.PerformActionsParameters;

import java.util.List;

public class NoneSourceActions extends PerformActionsParameters.SourceActions {
    private final List<NoneSourceAction> actions;

    public NoneSourceActions(List<NoneSourceAction> actions) {
        super("none");
        this.actions = actions;
    }

    public List<NoneSourceAction> getActions() {
        return actions;
    }
}
