package wd4j.impl.webdriver.type.script.parameters;

import wd4j.impl.webdriver.command.request.BrowsingContext;
import wd4j.impl.webdriver.type.script.PreloadScript;
import wd4j.impl.websocket.Command;

public class GetRealmsParameters implements Command.Params {
    public final BrowsingContext browsingContext;
    public final RealmType type;

    public GetRealmsParameters(BrowsingContext browsingContext, RealmType type) {
        this.browsingContext = browsingContext;
        this.type = type;
    }

    public BrowsingContext getBrowsingContext() {
        return browsingContext;
    }

    public RealmType getType() {
        return type;
    }
}
