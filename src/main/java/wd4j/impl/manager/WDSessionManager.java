package wd4j.impl.manager;

import wd4j.impl.markerInterfaces.WDModule;
import wd4j.impl.webdriver.command.request.WDSessionRequest;
import wd4j.impl.webdriver.command.response.WDEmptyResult;
import wd4j.impl.webdriver.command.response.WDSessionResult;
import wd4j.impl.websocket.WebSocketManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class WDSessionManager implements WDModule {
    private final WebSocketManager webSocketManager;
    private final Set<String> subscribedEvents = new HashSet<>();

    /**
     * Erstellt eine neue Session und gibt diese zurück.
     * Da einige Browser einen Standard-Kontext erstellen, wird mit diesem direkt ein neuer Browsing-Kontext erstellt.
     * Damit das Verhalten konsistent ist, wird ein neuer Kontext erstellt, wenn kein Standard-Kontext gefunden wird.
     *
     * @param webSocketManager The high-level api
     * @return Die erstellte Session
     */
    public WDSessionManager(WebSocketManager webSocketManager) throws ExecutionException, InterruptedException {
        this.webSocketManager = webSocketManager;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Event Handlers
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Commands
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Ruft den Status der WebDriver BiDi Session ab.
     */
    public WDSessionResult.StatusSessionResult status() {
        return webSocketManager.sendAndWaitForResponse(new WDSessionRequest.Status(), WDSessionResult.StatusSessionResult.class);
    }

    // new() - Since plain "new" is a reserved word in Java!
    /**
     * Erstellt eine neue Session mit dem gegebenen Browser.
     */
    public WDSessionResult.NewSessionResult newSession(String browserName) {
        return webSocketManager.sendAndWaitForResponse(new WDSessionRequest.New(browserName), WDSessionResult.NewSessionResult.class);
    }


    // end() - In corespondance to new!
    /**
     * Beendet die aktuelle WebDriver BiDi Session.
     */
    public void endSession() {
        webSocketManager.sendAndWaitForResponse(new WDSessionRequest.End(), WDEmptyResult.class);
    }


    /**
     * Abonniert WebDriver BiDi Events.
     * Falls bereits abonniert, wird das Event nicht erneut angefordert.
     */
    public WDSessionResult.SubscribeSessionResult subscribe(List<String> events) {
        WDSessionResult.SubscribeSessionResult result = null;
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
            result = webSocketManager.sendAndWaitForResponse(
                    new WDSessionRequest.Subscribe(newEvents), WDSessionResult.SubscribeSessionResult.class);
            subscribedEvents.addAll(newEvents);
            System.out.println("Subscribed to new events: " + newEvents);
        }

        return result;
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
            webSocketManager.sendAndWaitForResponse(new WDSessionRequest.Unsubscribe(eventsToRemove), WDEmptyResult.class);
            subscribedEvents.removeAll(eventsToRemove);
            System.out.println("Unsubscribed from events: " + eventsToRemove);
        }
    }
}