package wd4j.impl.module.type;

public class BrowsingContextNavigationInfo {
    private final String navigationId;
    private final String url;
    private final BrowsingContextReadinessState readinessState;

    public BrowsingContextNavigationInfo(String navigationId, String url, BrowsingContextReadinessState readinessState) {
        if (navigationId == null || navigationId.isEmpty()) {
            throw new IllegalArgumentException("Navigation ID must not be null or empty.");
        }
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("URL must not be null or empty.");
        }
        this.navigationId = navigationId;
        this.url = url;
        this.readinessState = readinessState;
    }

    public String getNavigationId() {
        return navigationId;
    }

    public String getUrl() {
        return url;
    }

    public BrowsingContextReadinessState getReadinessState() {
        return readinessState;
    }
}