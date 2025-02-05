package wd4j.impl.webdriver.type.webExtension.extensionData;

import wd4j.impl.webdriver.type.webExtension.ExtensionData;

public class ExtensionBase64Encoded implements ExtensionData {
    private final String type = "base64";
    private final String value;

    public ExtensionBase64Encoded(String value) {
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }
}
