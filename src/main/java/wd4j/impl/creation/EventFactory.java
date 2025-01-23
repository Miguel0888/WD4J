package wd4j.impl.creation;

import com.google.gson.JsonObject;
import wd4j.impl.generic.Event;

public class EventFactory {

    // Sample
    public static Event createEvent(JsonObject rawEvent) {
        String type = rawEvent.get("type").getAsString();
        JsonObject data = rawEvent.get("data").getAsJsonObject();

        // switch (type) {
        //     case "browsingContext.load":
        //         return new BrowsingContextLoadEvent(type, data);
        //     case "log.entryAdded":
        //         return new LogEntryAddedEvent(type, data);
        //     default:
        //         return new WebDriverEvent(type, data);
        // }
        return null;
    }
}

