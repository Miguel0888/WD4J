package de.bund.zrb.webdriver.command.request.parameters.script;

import de.bund.zrb.webdriver.type.browsingContext.WDBrowsingContext;
import de.bund.zrb.webdriver.type.script.WDRealmType;
import de.bund.zrb.websocket.WDCommand;

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
