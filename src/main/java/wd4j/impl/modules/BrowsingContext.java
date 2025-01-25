package wd4j.impl.modules;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import wd4j.core.CommandImpl;
import wd4j.core.WebSocketConnection;
import wd4j.helper.JsonObjectBuilder;
import wd4j.impl.generic.Module;
import wd4j.impl.generic.Event;

import java.util.concurrent.ExecutionException;

public class BrowsingContext implements Module {

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// ToDo: Import the Specs: Commands (Methodes) and Types (Classes)
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final WebSocketConnection webSocketConnection;
    private final String contextId;

    public BrowsingContext(WebSocketConnection webSocketConnection) {
        this.webSocketConnection = webSocketConnection;
        this.contextId = createContext();
    }

    public BrowsingContext(WebSocketConnection webSocketConnection, String contextId) {
        this.webSocketConnection = webSocketConnection;
        this.contextId = contextId;
    }

    /*
     * Required for Firefox ESR ?
     */
    // Hilfsmethode: Neuen Context erstellen
    private String createContext() {
        CommandImpl createContextCommandImpl = new CommandImpl(
                webSocketConnection,
                "browsingContext.create",
                new JsonObjectBuilder()
                        .addProperty("type", "tab") // Standardmäßig einen neuen Tab erstellen
                        .build()
        );

        try {
            String response = webSocketConnection.send(createContextCommandImpl);

            JsonObject jsonResponse = new Gson().fromJson(response, JsonObject.class);
            JsonObject result = jsonResponse.getAsJsonObject("result");

            if (result != null && result.has("context")) {
                String contextId = result.get("context").getAsString();
                System.out.println("--- Neuer Context erstellt: " + contextId);
                return contextId;
            }
        } catch (RuntimeException e) {
            System.out.println("Error creating context: " + e.getMessage());
            throw e;
        }

        throw new IllegalStateException("Failed to create new context."); // ToDo: Maybe return null instead?
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

        CommandImpl navigateCommandImpl = new CommandImpl(
                webSocketConnection,
                "browsingContext.navigate",
                params
        );

        // Send the command and wait for the response
        webSocketConnection.sendAsync(navigateCommandImpl);
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