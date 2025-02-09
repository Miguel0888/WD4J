package wd4j.impl.webdriver.command.request.parameters.browsingContext;

import wd4j.impl.webdriver.type.script.RemoteReference;

public class ElementClipRectangle implements ClipRectangle {
    private final String type = "element";
    private final RemoteReference.SharedReference element;

    public ElementClipRectangle(RemoteReference.SharedReference element) {
        this.element = element;
    }

    public String getType() {
        return type;
    }

    public RemoteReference.SharedReference getElement() {
        return element;
    }
}
