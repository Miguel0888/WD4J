package wd4j.impl.webdriver.type.browsingContext;

import wd4j.impl.markerInterfaces.WDType;
import wd4j.impl.webdriver.type.browser.WDClientWindow;
import wd4j.impl.webdriver.type.browser.WDUserContext;

import java.util.List;

public class WDInfo implements WDType<WDInfo> {
    private final List<WDInfo> children;
    private final WDClientWindow WDClientWindow;
    private final WDBrowsingContext context;
    private final WDBrowsingContext originalOpener;
    private final String url;
    private final WDUserContext WDUserContext;
    private final WDBrowsingContext parent; // optional

    public WDInfo(List<WDInfo> children, WDClientWindow WDClientWindow, WDBrowsingContext context, WDBrowsingContext originalOpener, String url, WDUserContext WDUserContext, WDBrowsingContext parent) {
        this.children = children;
        this.WDClientWindow = WDClientWindow;
        this.context = context;
        this.originalOpener = originalOpener;
        this.url = url;
        this.WDUserContext = WDUserContext;
        this.parent = parent;
    }

    public List<WDInfo> getChildren() {
        return children;
    }

    public WDClientWindow getClientWindow() {
        return WDClientWindow;
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
        return WDUserContext;
    }

    public WDBrowsingContext getParent() {
        return parent;
    }
}