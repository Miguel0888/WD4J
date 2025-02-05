package wd4j.impl.webdriver.type.webExtension;

import wd4j.impl.markerInterfaces.Type;

public class Extension implements Type {
    private final String id;
    private final String name;
    private final boolean enabled;
    private final String version;

    /**
     * Constructor for the WebExtension.Extension type.
     *
     * @param id      The unique ID of the extension.
     * @param name    The name of the extension.
     * @param enabled Whether the extension is enabled.
     * @param version The version of the extension.
     */
    public Extension(String id, String name, boolean enabled, String version) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Extension ID must not be null or empty.");
        }
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Extension name must not be null or empty.");
        }
        if (version == null || version.isEmpty()) {
            throw new IllegalArgumentException("Extension version must not be null or empty.");
        }
        this.id = id;
        this.name = name;
        this.enabled = enabled;
        this.version = version;
    }

    // Getters for all fields

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "Extension{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", enabled=" + enabled +
                ", version='" + version + '\'' +
                '}';
    }
}
