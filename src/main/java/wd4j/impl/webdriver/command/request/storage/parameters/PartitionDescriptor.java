package wd4j.impl.webdriver.command.request.storage.parameters;

public abstract class PartitionDescriptor {
    private final String type;

    public PartitionDescriptor(String type) {
        this.type = type;
    }
}
