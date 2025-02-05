package wd4j.impl.webdriver.type.browsingContext.parameters;

import wd4j.impl.webdriver.type.script.remoteReference.SharedReference;

public class ElementClipRectangle implements ClipRectangle {
    private final String type = "element";
    private final SharedReference element;

    public ElementClipRectangle(SharedReference element) {
        this.element = element;
    }

    public String getType() {
        return type;
    }

    public SharedReference getElement() {
        return element;
    }
}
