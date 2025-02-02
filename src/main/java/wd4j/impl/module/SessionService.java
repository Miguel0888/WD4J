package wd4j.impl.module;

import wd4j.core.CommandImpl;
import wd4j.core.WebSocketConnection;
import wd4j.impl.generic.Event;
import wd4j.impl.generic.Module;
import wd4j.impl.module.command.Session;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class SessionService implements Module {
    private final WebSocketConnection webSocketConnection;
    private final Set<String> subscribedEvents = new HashSet<>();

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
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Event Handlers
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


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
     * Abonniert WebDriver BiDi Events.
     * Falls bereits abonniert, wird das Event nicht erneut angefordert.
     */
    public void subscribe(List<String> events) {
        if (events == null || events.isEmpty()) {
            throw new IllegalArgumentException("Events list must not be null or empty.");
        }

        // Nur Events abonnieren, die noch nicht aktiv sind
        List<String> newEvents = new ArrayList<>();
        for (String event : events) {
            if (!subscribedEvents.contains(event)) {
                newEvents.add(event);
            }
        }

        if (!newEvents.isEmpty()) {
            webSocketConnection.send(new Session.Subscribe(newEvents));
            subscribedEvents.addAll(newEvents);
            System.out.println("Subscribed to new events: " + newEvents);
        }
    }

    /**
     * Entfernt die Event-Subscription für WebDriver BiDi Events.
     */
    public void unsubscribe(List<String> events) {
        if (events == null || events.isEmpty()) {
            throw new IllegalArgumentException("Events list must not be null or empty.");
        }

        List<String> eventsToRemove = new ArrayList<>();
        for (String event : events) {
            if (subscribedEvents.contains(event)) {
                eventsToRemove.add(event);
            }
        }

        if (!eventsToRemove.isEmpty()) {
            webSocketConnection.send(new Session.Unsubscribe(eventsToRemove));
            subscribedEvents.removeAll(eventsToRemove);
            System.out.println("Unsubscribed from events: " + eventsToRemove);
        }
    }
}