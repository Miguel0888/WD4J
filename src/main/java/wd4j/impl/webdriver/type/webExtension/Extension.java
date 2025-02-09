package wd4j.impl.webdriver.type.webExtension;

import wd4j.impl.markerInterfaces.Type;

public class Extension implements Type {
    private final String extension;

    public Extension(String extension) {
        if (extension == null || extension.isEmpty()) {
            throw new IllegalArgumentException("Extension must not be null or empty.");
        }
        this.extension = extension;
    }

    public String getExtension() {
        return extension;
    }
}
