package wd4j.impl.modules;

import com.google.gson.JsonObject;
import wd4j.core.WebSocketConnection;
import wd4j.impl.generic.Module;
import wd4j.impl.generic.Event;
import wd4j.core.Command;

import java.util.concurrent.ExecutionException;

public class BrowsingContext implements Module {

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// ToDo: Import the Specs: Commands (Methodes) and Types (Classes)
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final WebSocketConnection connection;
    private final String contextId;

    // public BrowsingContext(WebSocketConnection connection) {
    //     this.connection = connection;
    // }

    public BrowsingContext(WebSocketConnection connection, String contextId) {
        this.connection = connection;
        this.contextId = contextId;
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// ToDo: Commands (Methods)
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // ToDo: Move create context from BiDiWebDriver to here!

    // public CompletableFuture<String> createContext() {
    //     JsonObject command = new JsonObject();
    //     command.addProperty("method", "browsingContext.create");
    //     return connection.sendAsync(command);
    // }

    /**
     * Navigates to the given URL within this browsing context.
     *
     * @param url The target URL to navigate to.
     * @return The response of the navigation command.
     * @throws ExecutionException   if an error occurs during execution.
     * @throws InterruptedException if the operation is interrupted.
     */
    public void navigate(String url) throws ExecutionException, InterruptedException {
        // Create the command payload
        JsonObject params = new JsonObject();
        params.addProperty("url", url);
        params.addProperty("context", contextId);

        Command navigateCommand = new Command(
                connection,
                "browsingContext.navigate",
                params
        );

        // Send the command and wait for the response
        connection.sendAsync(navigateCommand);
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