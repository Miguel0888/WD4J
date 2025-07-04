package de.bund.zrb.mapping;

import com.google.gson.Gson;
import de.bund.zrb.support.mapping.GsonMapperFactory;
import de.bund.zrb.type.browsingContext.WDNavigation;
import de.bund.zrb.type.log.WDLogEntry;

class GsonMapperFactoryTest {
    public static void main(String[] args) {
        Gson gson = GsonMapperFactory.getGson();

        // Test: Enum (Level)
        WDLogEntry.Level Level = WDLogEntry.Level.WARN;
        String jsonLevel = gson.toJson(Level);
        System.out.println("Serialized Level: " + jsonLevel); // Erwartet: "warn"

        WDLogEntry.Level deserializedLevel = gson.fromJson("\"warn\"", WDLogEntry.Level.class);
        System.out.println("Deserialized Level: " + deserializedLevel); // Erwartet: WARN

        // Test: StringWrapper (Navigation)
        WDNavigation WDNavigation = new WDNavigation("page-load");
        String jsonNavigation = gson.toJson(WDNavigation);
        System.out.println("Serialized Navigation: " + jsonNavigation); // Erwartet: "page-load"

        WDNavigation deserializedWDNavigation = gson.fromJson("\"page-load\"", WDNavigation.class);
        System.out.println("Deserialized Navigation: " + deserializedWDNavigation.value()); // Erwartet: page-load
    }
}