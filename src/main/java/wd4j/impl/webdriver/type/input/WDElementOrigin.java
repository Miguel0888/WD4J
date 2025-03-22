package wd4j.impl.webdriver.type.input;

import wd4j.impl.webdriver.command.request.parameters.input.sourceActions.Origin;
import wd4j.impl.webdriver.type.script.WDRemoteReference;

/**
 * `ElementOrigin` stellt einen Ursprung dar, der sich auf ein Element bezieht.
 */
public class WDElementOrigin implements Origin {
    private final String type = "element";
    private final WDRemoteReference.SharedReference element;

    public WDElementOrigin(WDRemoteReference.SharedReference element) {
        if (element == null) {
            throw new IllegalArgumentException("ElementReference must not be null.");
        }
        this.element = element;
    }

    public String getType() {
        return type;
    }

    public WDRemoteReference.SharedReference getElement() {
        return element;
    }
}
