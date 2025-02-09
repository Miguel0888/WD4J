package wd4j.impl.webdriver.type.input;

import com.google.gson.JsonObject;
import wd4j.impl.webdriver.type.script.remoteReference.SharedReference;

public class ElementOrigin {

    private final String type = "element";
    private final SharedReference element;

    public ElementOrigin(SharedReference element) {
        this.element = element;
    }

    public String getType() {
        return type;
    }

    public SharedReference getElement() {
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
