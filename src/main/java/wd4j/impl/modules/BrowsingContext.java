package wd4j.impl.modules;

public class BrowsingContext implements Module {

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// ToDo: Import the Specs: Commands (Methodes) and Types (Classes)
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final WebSocketConnection connection;

    public BrowsingContext(WebSocketConnection connection) {
        this.connection = connection;
    }

    public CompletableFuture<String> createContext() {
        JsonObject command = new JsonObject();
        command.addProperty("method", "browsingContext.create");
        return connection.sendAsync(command);
    }

    public CompletableFuture<String> navigate(String contextId, String url) {
        JsonObject command = new JsonObject();
        command.addProperty("method", "browsingContext.navigate");
        JsonObject params = new JsonObject();
        params.addProperty("context", contextId);
        params.addProperty("url", url);
        command.add("params", params);

        return connection.sendAsync(command);
    }
}