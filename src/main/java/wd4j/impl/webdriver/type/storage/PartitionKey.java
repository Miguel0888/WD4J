package wd4j.impl.webdriver.type.storage;

public class PartitionKey {
    private final String userContext; // Optional
    private final String sourceOrigin; // Optional

    public PartitionKey(String userContext, String sourceOrigin) {
        this.userContext = userContext;
        this.sourceOrigin = sourceOrigin;
    }

    public String getUserContext() {
        return userContext;
    }

    public String getSourceOrigin() {
        return sourceOrigin;
    }
}