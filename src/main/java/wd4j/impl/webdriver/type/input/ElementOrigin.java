package wd4j.impl.webdriver.type.input;

import com.google.gson.JsonObject;
import wd4j.impl.webdriver.type.script.RemoteReference;

public class ElementOrigin {

    private final String type = "element";
    private final RemoteReference.SharedReference element;

    public ElementOrigin(RemoteReference.SharedReference element) {
        this.element = element;
    }

    public String getType() {
        return type;
    }

    public RemoteReference.SharedReference getElement() {
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
