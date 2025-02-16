package wd4j.impl.webdriver.mapping;

import com.google.gson.Gson;
import wd4j.impl.webdriver.type.browsingContext.WDNavigation;
import wd4j.impl.webdriver.type.log.WDLevel;

class GsonMapperFactoryTest {
    public static void main(String[] args) {
        Gson gson = GsonMapperFactory.getGson();

        // Test: Enum (Level)
        WDLevel WDLevel = WDLevel.WARN;
        String jsonLevel = gson.toJson(WDLevel);
        System.out.println("Serialized Level: " + jsonLevel); // Erwartet: "warn"

        WDLevel deserializedWDLevel = gson.fromJson("\"warn\"", WDLevel.class);
        System.out.println("Deserialized Level: " + deserializedWDLevel); // Erwartet: WARN

        // Test: StringWrapper (Navigation)
        WDNavigation WDNavigation = new WDNavigation("page-load");
        String jsonNavigation = gson.toJson(WDNavigation);
        System.out.println("Serialized Navigation: " + jsonNavigation); // Erwartet: "page-load"

        WDNavigation deserializedWDNavigation = gson.fromJson("\"page-load\"", WDNavigation.class);
        System.out.println("Deserialized Navigation: " + deserializedWDNavigation.value()); // Erwartet: page-load
    }
}