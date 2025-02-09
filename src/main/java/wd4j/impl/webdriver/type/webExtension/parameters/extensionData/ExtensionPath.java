package wd4j.impl.webdriver.type.webExtension.parameters.extensionData;

import wd4j.impl.webdriver.type.webExtension.parameters.ExtensionData;

public class ExtensionPath implements ExtensionData {
    private final String type = "path";
    private final String path;

    public ExtensionPath(String path) {
        this.path = path;
    }

    public String getType() {
        return type;
    }

    public String getPath() {
        return path;
    }
}
