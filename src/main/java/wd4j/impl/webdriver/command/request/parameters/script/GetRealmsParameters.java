package wd4j.impl.webdriver.command.request.parameters.script;

import wd4j.impl.webdriver.type.browsingContext.BrowsingContext;
import wd4j.impl.webdriver.type.script.RealmType;
import wd4j.impl.websocket.Command;

public class GetRealmsParameters implements Command.Params {
    public final BrowsingContext browsingContextRequest; // Optional
    public final RealmType type; // Optional

    public GetRealmsParameters() {
        this(null, null);
    }

    public GetRealmsParameters(BrowsingContext browsingContext, RealmType type) {
        this.browsingContextRequest = browsingContext;
        this.type = type;
    }

    public BrowsingContext getBrowsingContext() {
        return browsingContextRequest;
    }

    public RealmType getType() {
        return type;
    }
}
