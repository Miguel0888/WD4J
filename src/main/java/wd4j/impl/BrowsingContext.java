package wd4j.impl;

public class BrowsingContext {
    private final WebSocketConnection connection;

    public BrowsingContext(WebSocketConnection connection) {
        this.connection = connection;
    }

    public void navigate(String contextId, String url) {
        String command = String.format(
                "{\"id\":1,\"method\":\"browsingContext.navigate\",\"params\":{\"context\":\"%s\",\"url\":\"%s\"}}",
                contextId, url
        );
        connection.send(command);
    }

    public void closeContext(String contextId) {
        String command = String.format(
                "{\"id\":2,\"method\":\"browsingContext.close\",\"params\":{\"context\":\"%s\"}}",
                contextId
        );
        connection.send(command);
    }

    public void listContexts() {
        String command = "{\"id\":3,\"method\":\"browsingContext.getTree\",\"params\":{}}";
        connection.send(command);
    }
}
