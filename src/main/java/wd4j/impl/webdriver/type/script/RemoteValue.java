package wd4j.impl.webdriver.type.script;

public abstract class RemoteValue {
    private final String type;
    private final String handle;
    private final String internalId;

    protected RemoteValue(String type, String handle, String internalId) {
        if (type == null || type.isEmpty()) {
            throw new IllegalArgumentException("Type must not be null or empty.");
        }
        this.type = type;
        this.handle = handle;
        this.internalId = internalId;
    }

    public String getType() {
        return type;
    }

    public String getHandle() {
        return handle;
    }

    public String getInternalId() {
        return internalId;
    }

    @Override
    public String toString() {
        return "RemoteValue{" +
                "type='" + type + '\'' +
                ", handle='" + handle + '\'' +
                ", internalId='" + internalId + '\'' +
                '}';
    }
}
