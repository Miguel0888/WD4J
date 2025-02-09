package wd4j.impl.webdriver.command.request.input.parameters.sourceActions;

import wd4j.impl.webdriver.command.request.input.parameters.SourceActions;

import java.util.List;

public class PointerSourceActions extends SourceActions {
    public final String id;
    public final PointerParameters parameters;
    public final List<PointerSourceAction> actions;

    public PointerSourceActions(String id, PointerParameters parameters, List<PointerSourceAction> actions) {
        super("pointer");
        this.id = id;
        this.parameters = parameters;
        this.actions = actions;
    }

    public String getId() {
        return id;
    }

    public PointerParameters getParameters() {
        return parameters;
    }

    public List<PointerSourceAction> getActions() {
        return actions;
    }
}
