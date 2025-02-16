package wd4j.impl.webdriver.command.request.parameters.input;

import wd4j.impl.webdriver.type.browsingContext.WDBrowsingContext;
import wd4j.impl.websocket.WDCommand;

import java.util.List;

public class PerformActionsParameters implements WDCommand.Params {
    private final WDBrowsingContext WDBrowsingContext;
    private final List<SourceActions> actions;

    public PerformActionsParameters(WDBrowsingContext WDBrowsingContext, List<SourceActions> actions) {
        this.WDBrowsingContext = WDBrowsingContext;
        this.actions = actions;
    }

    public WDBrowsingContext getBrowsingContext() {
        return WDBrowsingContext;
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
