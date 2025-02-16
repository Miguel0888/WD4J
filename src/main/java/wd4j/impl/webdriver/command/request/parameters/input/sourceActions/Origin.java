package wd4j.impl.webdriver.command.request.parameters.input.sourceActions;

import wd4j.impl.webdriver.type.input.WDElementOrigin;

// ToDo: Maybe this is ElementOrigin instead of Origin?
public class Origin {
    private final String origin;
    private final WDElementOrigin WDElementOrigin;

    public Origin( String origin ) {
        this.origin = origin;
        this.WDElementOrigin = null;
    }

    public Origin( WDElementOrigin WDElementOrigin)
    {
        this.WDElementOrigin = WDElementOrigin;
        this.origin = null;
    }

    // ToDo: How to return the origin?
}
