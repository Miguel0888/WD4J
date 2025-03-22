package wd4j.impl.dto.command.request.parameters.browsingContext;

import wd4j.impl.dto.type.browser.WDUserContext;
import wd4j.impl.dto.type.browsingContext.WDBrowsingContext;
import wd4j.impl.websocket.WDCommand;

public class CreateParameters implements WDCommand.Params {
    private final CreateType type;
    private final WDBrowsingContext referenceContext; // optional
    private final Boolean background; // optional
    private final WDUserContext WDUserContext; // optional

    public CreateParameters(CreateType type) {
        this(type, null, null, null);
    }

    public CreateParameters(CreateType type, WDBrowsingContext referenceContext, Boolean background, WDUserContext WDUserContext) {
        this.type = type;
        this.referenceContext = referenceContext;
        this.background = background;
        this.WDUserContext = WDUserContext;
    }

    public CreateType getType() {
        return type;
    }

    public WDBrowsingContext getReferenceContext() {
        return referenceContext;
    }

    public Boolean isBackground() {
        return background;
    }

    public WDUserContext getUserContext() {
        return WDUserContext;
    }
}
