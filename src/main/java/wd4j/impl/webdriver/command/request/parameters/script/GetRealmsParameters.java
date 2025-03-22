package wd4j.impl.webdriver.command.request.parameters.script;

import wd4j.impl.webdriver.type.browsingContext.WDBrowsingContext;
import wd4j.impl.webdriver.type.script.WDRealmType;
import wd4j.impl.websocket.WDCommand;

public class GetRealmsParameters implements WDCommand.Params {
    public final WDBrowsingContext WDBrowsingContextRequest; // Optional
    public final WDRealmType type; // Optional

    public GetRealmsParameters() {
        this(null, null);
    }

    public GetRealmsParameters(WDBrowsingContext WDBrowsingContext, WDRealmType type) {
        this.WDBrowsingContextRequest = WDBrowsingContext;
        this.type = type;
    }

    public WDBrowsingContext getBrowsingContext() {
        return WDBrowsingContextRequest;
    }

    public WDRealmType getType() {
        return type;
    }
}
