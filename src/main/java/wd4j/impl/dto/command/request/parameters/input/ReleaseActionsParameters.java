package wd4j.impl.dto.command.request.parameters.input;

import wd4j.impl.dto.type.browsingContext.WDBrowsingContext;
import wd4j.impl.websocket.WDCommand;

public class ReleaseActionsParameters implements WDCommand.Params {
    private final WDBrowsingContext WDBrowsingContext;

    public ReleaseActionsParameters(WDBrowsingContext WDBrowsingContext) {
        this.WDBrowsingContext = WDBrowsingContext;
    }

    public WDBrowsingContext getBrowsingContext() {
        return WDBrowsingContext;
    }
}
