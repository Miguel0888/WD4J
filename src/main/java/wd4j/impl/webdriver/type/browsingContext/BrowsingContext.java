package wd4j.impl.webdriver.type.browsingContext;

public class BrowsingContext {
    private final String contextId;

    public BrowsingContext(String contextId) {
        if (contextId == null || contextId.isEmpty()) {
            throw new IllegalArgumentException("Context ID must not be null or empty.");
        }
        this.contextId = contextId;
    }

    public String getContextId() {
        return contextId;
    }
}
