package wd4j.impl.creation;

import com.google.gson.JsonObject;
import wd4j.impl.generic.Event;
import java.util.Optional;
import wd4j.impl.modules.BrowsingContext;

public class EventFactory {
    public static Optional<Event> fromJson(JsonObject json) {
        String type = json.get("type").getAsString();
        switch (type) {
            case "browsingContext.contextCreated":
                return Optional.of(new BrowsingContext.CreatedEvent(json));
            // Weitere Event-Typen hier...
            default:
                System.out.println("Unknown event type: " + type);
                return Optional.empty();
        }
    }
}

