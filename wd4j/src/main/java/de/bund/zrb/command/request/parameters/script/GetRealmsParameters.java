package de.bund.zrb.command.request.parameters.script;

import de.bund.zrb.type.browsingContext.WDBrowsingContext;
import de.bund.zrb.type.script.WDRealmType;
import de.bund.zrb.api.WDCommand;

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
