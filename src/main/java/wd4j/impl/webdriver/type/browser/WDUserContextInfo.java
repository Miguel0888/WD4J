package wd4j.impl.webdriver.type.browser;

import wd4j.impl.markerInterfaces.WDType;

// ToDo: How to implement this class correctly?
public class WDUserContextInfo implements WDType<WDUserContextInfo> {
    private final WDUserContext WDUserContext;

    public WDUserContextInfo(WDUserContext WDUserContext) {
        if (WDUserContext == null) {
            throw new IllegalArgumentException("UserContext must not be null.");
        }
        this.WDUserContext = WDUserContext;
    }

    public WDUserContext getUserContext() {
        return WDUserContext;
    }
}