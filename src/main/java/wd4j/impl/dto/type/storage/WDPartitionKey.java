package wd4j.impl.dto.type.storage;

public class WDPartitionKey {
    private final String userContext; // Optional
    private final String sourceOrigin; // Optional

    public WDPartitionKey(String userContext, String sourceOrigin) {
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