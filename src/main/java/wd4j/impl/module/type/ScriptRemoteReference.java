package wd4j.impl.module.type;

public class ScriptRemoteReference {
    private final String reference;

    public ScriptRemoteReference(String reference) {
        if (reference == null || reference.isEmpty()) {
            throw new IllegalArgumentException("Reference must not be null or empty.");
        }
        this.reference = reference;
    }

    public String getReference() {
        return reference;
    }
}