package de.bund.zrb.type.browser;

import de.bund.zrb.api.markerInterfaces.WDType;

// ToDo: How to implement this class correctly?
public class WDUserContextInfo implements WDType<WDUserContextInfo> {
    private final WDUserContext userContext;

    public WDUserContextInfo(WDUserContext userContext) {
        if (userContext == null) {
            throw new IllegalArgumentException("UserContext must not be null.");
        }
        this.userContext = userContext;
    }

    public WDUserContext getUserContext() {
        return userContext;
    }
}