package wd4j.impl.webdriver.command.request.parameters.storage;

import wd4j.impl.webdriver.type.browsingContext.BrowsingContext;

public abstract class PartitionDescriptor {
    private final String type;

    public PartitionDescriptor(String type) {
        this.type = type;
    }

    public static class BrowsingContextPartitionDescriptor extends PartitionDescriptor {
        private final BrowsingContext context;

        public BrowsingContextPartitionDescriptor(BrowsingContext context) {
            super("context");
            this.context = context;
        }

        public BrowsingContext getContext() {
            return context;
        }
    }

    public static class StorageKeyPartitionDescriptor extends PartitionDescriptor {
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
}