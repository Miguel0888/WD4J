package wd4j.impl.dto.type.browsingContext;

import wd4j.impl.markerInterfaces.WDType;
import wd4j.impl.dto.type.browser.WDClientWindow;
import wd4j.impl.dto.type.browser.WDUserContext;

import java.util.List;

public class WDInfo implements WDType<WDInfo> {
    private final List<WDInfo> children;
    private final WDClientWindow clientWindow;
    private final WDBrowsingContext context;
    private final WDBrowsingContext originalOpener;
    private final String url;
    private final WDUserContext userContext;
    private final WDBrowsingContext parent; // optional

    public WDInfo(List<WDInfo> children, WDClientWindow clientWindow, WDBrowsingContext context, WDBrowsingContext originalOpener, String url, WDUserContext userContext, WDBrowsingContext parent) {
        this.children = children;
        this.clientWindow = clientWindow;
        this.context = context;
        this.originalOpener = originalOpener;
        this.url = url;
        this.userContext = userContext;
        this.parent = parent;
    }

    public List<WDInfo> getChildren() {
        return children;
    }

    public WDClientWindow getClientWindow() {
        return clientWindow;
    }

    public WDBrowsingContext getContext() {
        return context;
    }

    public WDBrowsingContext getOriginalOpener() {
        return originalOpener;
    }

    public String getUrl() {
        return url;
    }

    public WDUserContext getUserContext() {
        return userContext;
    }

    public WDBrowsingContext getParent() {
        return parent;
    }
}