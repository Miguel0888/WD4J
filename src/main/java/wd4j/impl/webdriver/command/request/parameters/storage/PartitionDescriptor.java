package wd4j.impl.webdriver.command.request.parameters.storage;

public abstract class PartitionDescriptor {
    private final String type;

    public PartitionDescriptor(String type) {
        this.type = type;
    }
}
