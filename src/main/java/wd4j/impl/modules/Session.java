package wd4j.impl.modules;

import wd4j.impl.WebSocketConnection;

public class Session implements Module {
    private final WebSocketConnection connection;

    public Session(WebSocketConnection connection) {
        this.connection = connection;

        // Register for events:
        connection.addEventListener(event -> {
            System.out.println(event -> onEvent(event));
        });
    }

    private void onEvent(Event event) {
        switch (event.getType()) {
            case "session.created":
                System.out.println("Session created: " + event.getData());
                break;
            case "session.deleted":
                System.out.println("Session deleted: " + event.getData());
                break;
            default:
                System.out.println("Unhandled event: " + event.getType());
        }
    }

    public void status()
    {}

    // new() - Since plain "new" is a reserved word in Java!
    public CompletableFuture<String> newSession(String browserName) {
        JsonObject command = new JsonObject();
        command.addProperty("method", "session.new");
        JsonObject params = new JsonObject();
        JsonObject capabilities = new JsonObject();
        capabilities.addProperty("browserName", browserName);
        params.add("capabilities", capabilities);
        command.add("params", params);

        return connection.sendAsync(command);
    }

    // end() - In corespondance to new!
    public CompletableFuture<String> endSession() {
        JsonObject command = new JsonObject();
        command.addProperty("method", "session.delete");

        return connection.sendAsync(command);
    }

    public void subscribe()
    {}
    public void unsubscribe()
    {}

    public static class CapabilitesRequest implements Type {
        // ToDo
    }
    public static class CapabilityRequest implements Type {
        // ToDo
    }
    public static class ProxyConfiguration implements Type {
        // ToDo
    }
    public static class UserPromptHandler implements Type {
        // ToDo
    }
    public static class UserPromptHandlerType implements Type {
        // ToDo
    }
    public static class Subscription implements Type {
        // ToDo
    }
    public static class SubscriptionRequest implements Type {
        // ToDo
    }
    public static class UnsubscribeByIdRequest implements Type {
        // ToDo
    }
    public static class UnsubscribeByAttributeRequest implements Type {
        // ToDo
    }
}