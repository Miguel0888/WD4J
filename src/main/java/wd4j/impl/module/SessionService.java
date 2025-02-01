package wd4j.impl.module;

import wd4j.core.CommandImpl;
import wd4j.core.WebSocketConnection;
import wd4j.impl.generic.Event;
import wd4j.impl.generic.Module;
import wd4j.impl.module.command.Session;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class SessionService implements Module {
    private final WebSocketConnection webSocketConnection;

    /**
     * Erstellt eine neue Session und gibt diese zurück.
     * Da einige Browser einen Standard-Kontext erstellen, wird mit diesem direkt ein neuer Browsing-Kontext erstellt.
     * Damit das Verhalten konsistent ist, wird ein neuer Kontext erstellt, wenn kein Standard-Kontext gefunden wird.
     *
     * @param webSocketConnection Der Browsertyp
     * @return Die erstellte Session
     */
    public SessionService(WebSocketConnection webSocketConnection) throws ExecutionException, InterruptedException {
        this.webSocketConnection = webSocketConnection;

        // Register for events
        this.webSocketConnection.addEventListener(event -> onEvent(event));
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Event Handlers
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


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

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Commands
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<String> status() {
        return webSocketConnection.sendAsync(new Session.Status());
    }

    // new() - Since plain "new" is a reserved word in Java!
    public CompletableFuture<String> newSession(String browserName) {
        return webSocketConnection.sendAsync(new Session.New(browserName));
    }

    // end() - In corespondance to new!
    public CompletableFuture<String> endSession() {
        // ToDo: Maybe close all BrowsingContexts?
        CommandImpl endSessionCommand = new Session.End();
    
        return webSocketConnection.sendAsync(endSessionCommand);
    }

    /**
     * Subscribes to specific WebDriver BiDi events.
     *
     * @param events A list of event names to subscribe to.
     * @throws RuntimeException if the subscription fails.
     */
    public void subscribe(List<String> events) {
        if (events == null || events.isEmpty()) {
            throw new IllegalArgumentException("Events list must not be null or empty.");
        }

        try {
            webSocketConnection.send(new Session.Subscribe(events));
            System.out.println("Subscribed to events: " + events);
        } catch (RuntimeException e) {
            System.out.println("Error subscribing to events: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Unsubscribes from specific WebDriver BiDi events.
     *
     * @param events A list of event names to unsubscribe from.
     * @throws RuntimeException if the unsubscription fails.
     */
    public void unsubscribe(List<String> events) {
        if (events == null || events.isEmpty()) {
            throw new IllegalArgumentException("Events list must not be null or empty.");
        }

        try {
            webSocketConnection.send(new Session.Unsubscribe(events));
            System.out.println("Unsubscribed from events: " + events);
        } catch (RuntimeException e) {
            System.out.println("Error unsubscribing from events: " + e.getMessage());
            throw e;
        }
    }
}