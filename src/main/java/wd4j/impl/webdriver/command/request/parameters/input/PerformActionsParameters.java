package wd4j.impl.webdriver.command.request.parameters.input;

import wd4j.impl.webdriver.type.browsingContext.BrowsingContext;
import wd4j.impl.websocket.Command;

import java.util.List;

public class PerformActionsParameters implements Command.Params {
    private final BrowsingContext browsingContext;
    private final List<SourceActions> actions;

    public PerformActionsParameters(BrowsingContext browsingContext, List<SourceActions> actions) {
        this.browsingContext = browsingContext;
        this.actions = actions;
    }

    public BrowsingContext getBrowsingContext() {
        return browsingContext;
    }

    public abstract static class SourceActions {
        private String type;

        public SourceActions(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }

    }
}
