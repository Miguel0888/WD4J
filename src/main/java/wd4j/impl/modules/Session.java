package wd4j.impl.modules;

import com.google.gson.JsonObject;
import wd4j.helper.JsonObjectBuilder;
import wd4j.core.WebSocketConnection;
import wd4j.impl.generic.Event;
import wd4j.impl.generic.Module;
import wd4j.impl.generic.Type;

import wd4j.core.Command;

import java.util.concurrent.CompletableFuture;

public class Session implements Module {
    private final WebSocketConnection webSocketConnection;

    public Session(WebSocketConnection webSocketConnection) {
        this.webSocketConnection = webSocketConnection;

        // Register for events
        webSocketConnection.addEventListener(event -> onEvent(event));
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
        Command newSessionCommand = new Command(
            webSocketConnection,
            "session.new",
            new JsonObjectBuilder()
                .add("capabilities", new JsonObjectBuilder()
                    .addProperty("browserName", browserName)
                    .build())
                .build()
        );
    
        return webSocketConnection.sendAsync(newSessionCommand);
    }    

    // end() - In corespondance to new!
    public CompletableFuture<String> endSession() {
        Command endSessionCommand = new Command(
            webSocketConnection,
            "session.delete",
            new JsonObject() // Kein Parameter erforderlich
        );
    
        return webSocketConnection.sendAsync(endSessionCommand);
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