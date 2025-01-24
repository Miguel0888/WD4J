package wd4j.impl.modules;

import com.google.gson.JsonObject;
import wd4j.impl.WebSocketConnection;
import wd4j.impl.generic.Module;
import wd4j.impl.generic.Event;

import java.util.concurrent.CompletableFuture;

public class BrowsingContext implements Module {

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// ToDo: Import the Specs: Commands (Methodes) and Types (Classes)
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final WebSocketConnection connection;

    public BrowsingContext(WebSocketConnection connection) {
        this.connection = connection;
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// ToDo: Commands (Methods)
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // public CompletableFuture<String> createContext() {
    //     JsonObject command = new JsonObject();
    //     command.addProperty("method", "browsingContext.create");
    //     return connection.sendAsync(command);
    // }

    public CompletableFuture<String> navigate(String contextId, String url) {
        // JsonObject command = new JsonObject();
        // command.addProperty("method", "browsingContext.navigate");
        // JsonObject params = new JsonObject();
        // params.addProperty("context", contextId);
        // params.addProperty("url", url);
        // command.add("params", params);

        // return connection.sendAsync(command);
        return null;
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Events (Classes)
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    public static class CreatedEvent implements Event {
        private final JsonObject data;
        private final String type;
        private final String contextId;
        private final String url;

        public CreatedEvent(JsonObject json) {
            this.data = json;
            this.type = json.get("type").getAsString();
            this.contextId = json.getAsJsonObject("context").get("contextId").getAsString();
            this.url = json.getAsJsonObject("context").get("url").getAsString();
        }

        public String getContextId() {
            return contextId;
        }

        public String getUrl() {
            return url;
        }

        @Override
        public String toString() {
            return "BrowsingContext.CreatedEvent{contextId=" + contextId + ", url='" + url + "'}";
        }

        @Override
        public String getType() {
            return type;
        }

        @Override
        public JsonObject getData() {
            return data;
        }
    }
}