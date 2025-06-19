package de.bund.zrb.webdriver.type.input;

import de.bund.zrb.webdriver.command.request.parameters.input.sourceActions.Origin;
import de.bund.zrb.webdriver.type.script.WDRemoteReference;

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
