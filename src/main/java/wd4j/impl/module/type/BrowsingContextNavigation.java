package wd4j.impl.module.type;

public class BrowsingContextNavigation {
    private final String context;
    private final String navigationId;

    public BrowsingContextNavigation(String context, String navigationId) {
        if (context == null || context.isEmpty()) {
            throw new IllegalArgumentException("Context must not be null or empty.");
        }
        this.context = context;
        this.navigationId = navigationId;
    }

    public String getContext() {
        return context;
    }

    public String getNavigationId() {
        return navigationId;
    }
}