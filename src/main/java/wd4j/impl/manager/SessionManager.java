package wd4j.impl.manager;

import wd4j.impl.webdriver.command.request.helper.CommandImpl;
import wd4j.impl.markerInterfaces.Module;
import wd4j.impl.webdriver.command.request.SessionRequest;
import wd4j.impl.playwright.WebSocketImpl;
import wd4j.impl.websocket.CommunicationManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class SessionManager implements Module {
    private final CommunicationManager communicationManager;
    private final Set<String> subscribedEvents = new HashSet<>();

    /**
     * Erstellt eine neue Session und gibt diese zurück.
     * Da einige Browser einen Standard-Kontext erstellen, wird mit diesem direkt ein neuer Browsing-Kontext erstellt.
     * Damit das Verhalten konsistent ist, wird ein neuer Kontext erstellt, wenn kein Standard-Kontext gefunden wird.
     *
     * @param communicationManager The high-level api
     * @return Die erstellte Session
     */
    public SessionManager(CommunicationManager communicationManager) throws ExecutionException, InterruptedException {
        this.communicationManager = communicationManager;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Event Handlers
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Commands
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public String status() {
        return communicationManager.getWebSocket().sendAndWaitForResponse(new SessionRequest.Status());
    }

    // new() - Since plain "new" is a reserved word in Java!
    public String newSession(String browserName) {
        // ToDo. Should Response DTO
        return communicationManager.getWebSocket().sendAndWaitForResponse(new SessionRequest.New(browserName));
    }

    // end() - In corespondance to new!
    public String endSession() {
        // ToDo: Maybe close all BrowsingContexts?
        CommandImpl endSessionCommand = new SessionRequest.End();
    
        return communicationManager.getWebSocket().sendAndWaitForResponse(endSessionCommand);
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
            communicationManager.getWebSocket().sendAndWaitForResponse(new SessionRequest.Subscribe(newEvents));
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
            communicationManager.getWebSocket().sendAndWaitForResponse(new SessionRequest.Unsubscribe(eventsToRemove));
            subscribedEvents.removeAll(eventsToRemove);
            System.out.println("Unsubscribed from events: " + eventsToRemove);
        }
    }
}