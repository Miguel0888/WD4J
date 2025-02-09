package wd4j.impl.webdriver.command.request.parameters.script;

import wd4j.impl.webdriver.command.request.BrowsingContextRequest;
import wd4j.impl.webdriver.type.script.RealmType;
import wd4j.impl.websocket.Command;

public class GetRealmsParameters implements Command.Params {
    public final BrowsingContextRequest browsingContextRequest;
    public final RealmType type;

    public GetRealmsParameters(BrowsingContextRequest browsingContextRequest, RealmType type) {
        this.browsingContextRequest = browsingContextRequest;
        this.type = type;
    }

    public BrowsingContextRequest getBrowsingContext() {
        return browsingContextRequest;
    }

    public RealmType getType() {
        return type;
    }
}
