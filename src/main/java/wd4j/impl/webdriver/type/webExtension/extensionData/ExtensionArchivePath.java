package wd4j.impl.webdriver.type.webExtension.extensionData;

import wd4j.impl.webdriver.type.webExtension.ExtensionData;

public class ExtensionArchivePath implements ExtensionData {
    private final String type = "archivePath";
    private final String path;

    public ExtensionArchivePath(String path) {
        this.path = path;
    }

    public String getType() {
        return type;
    }

    public String getPath() {
        return path;
    }
}
