package wd4j.impl.webdriver.command.request.parameters.storage.partitionDescriptor;

import wd4j.impl.webdriver.command.request.parameters.storage.SetCookieParameters;

public class StorageKeyPartitionDescriptor extends SetCookieParameters.PartitionDescriptor {
    private final String userContext;
    private final String sourceOrigin;

    public StorageKeyPartitionDescriptor(String userContext, String sourceOrigin) {
        super("storageKey");
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
