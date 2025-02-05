package wd4j.impl.webdriver.type.storage.partitionDescriptor;

import wd4j.impl.webdriver.type.storage.PartitionDescriptor;

public class StorageKeyPartitionDescriptor extends PartitionDescriptor {
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
