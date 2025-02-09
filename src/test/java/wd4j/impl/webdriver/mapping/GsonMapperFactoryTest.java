package wd4j.impl.webdriver.mapping;

import com.google.gson.Gson;
import wd4j.impl.webdriver.type.browsingContext.Navigation;
import wd4j.impl.webdriver.type.log.Level;

import static org.junit.jupiter.api.Assertions.*;

class GsonMapperFactoryTest {
    public static void main(String[] args) {
        Gson gson = GsonMapperFactory.getGson();

        // Test: Enum (Level)
        Level level = Level.WARN;
        String jsonLevel = gson.toJson(level);
        System.out.println("Serialized Level: " + jsonLevel); // Erwartet: "warn"

        Level deserializedLevel = gson.fromJson("\"warn\"", Level.class);
        System.out.println("Deserialized Level: " + deserializedLevel); // Erwartet: WARN

        // Test: StringWrapper (Navigation)
        Navigation navigation = new Navigation("page-load");
        String jsonNavigation = gson.toJson(navigation);
        System.out.println("Serialized Navigation: " + jsonNavigation); // Erwartet: "page-load"

        Navigation deserializedNavigation = gson.fromJson("\"page-load\"", Navigation.class);
        System.out.println("Deserialized Navigation: " + deserializedNavigation.value()); // Erwartet: page-load
    }
}