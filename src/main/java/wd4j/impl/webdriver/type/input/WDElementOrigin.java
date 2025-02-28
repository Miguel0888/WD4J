package wd4j.impl.webdriver.type.input;

import wd4j.impl.webdriver.type.script.WDRemoteReference;

public class WDElementOrigin {

    private final String type = "element";
    private final WDRemoteReference.SharedReference element;

    public WDElementOrigin(WDRemoteReference.SharedReference element) {
        this.element = element;
    }

    public String getType() {
        return type;
    }

    public WDRemoteReference.SharedReference getElement() {
        return element;
    }

    // ToDo: Implement toJson() methods ???
//    public JsonObject toJson() {
//        JsonObject json = new JsonObject();
//        json.addProperty("type", type);
//        json.add("element", element.toJson());
//        return json;
//    }
}
