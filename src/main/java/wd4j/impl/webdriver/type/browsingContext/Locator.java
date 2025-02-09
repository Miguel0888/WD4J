package wd4j.impl.webdriver.type.browsingContext;

import com.google.gson.JsonObject;

public interface Locator<T> {
   String getType();
   T getValue();
}
