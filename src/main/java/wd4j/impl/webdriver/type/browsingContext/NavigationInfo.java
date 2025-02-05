package wd4j.impl.webdriver.type.browsingContext;

public class NavigationInfo {
    private final String navigationId;
    private final String url;
    private final ReadinessState readinessState;

    public NavigationInfo(String navigationId, String url, ReadinessState readinessState) {
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

    public ReadinessState getReadinessState() {
        return readinessState;
    }
}