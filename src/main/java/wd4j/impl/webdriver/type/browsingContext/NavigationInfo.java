package wd4j.impl.webdriver.type.browsingContext;

public class NavigationInfo {
    private final BrowsingContext context;
    private final Navigation navigation;
    private final char timestamp;
    private final String url;

    public NavigationInfo(BrowsingContext context, Navigation navigation, char timestamp, String url) {
        this.context = context;
        this.navigation = navigation;
        this.timestamp = timestamp;
        this.url = url;
    }

    public BrowsingContext getContext() {
        return context;
    }

    public Navigation getNavigation() {
        return navigation;
    }

    public char getTimestamp() {
        return timestamp;
    }

    public String getUrl() {
        return url;
    }
}