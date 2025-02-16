package wd4j.impl.manager;

import wd4j.impl.webdriver.command.request.helper.CommandImpl;
import wd4j.impl.markerInterfaces.Module;
import wd4j.impl.webdriver.command.request.SessionRequest;
import wd4j.impl.webdriver.command.response.EmptyResult;
import wd4j.impl.webdriver.command.response.SessionResult;
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

    /**
     * Ruft den Status der WebDriver BiDi Session ab.
     */
    public SessionResult.StatusResult status() {
        return communicationManager.sendAndWaitForResponse(new SessionRequest.Status(), SessionResult.StatusResult.class);
    }

    // new() - Since plain "new" is a reserved word in Java!
    /**
     * Erstellt eine neue Session mit dem gegebenen Browser.
     */
    public SessionResult.NewResult newSession(String browserName) {
        return communicationManager.sendAndWaitForResponse(new SessionRequest.New(browserName), SessionResult.NewResult.class);
    }


    // end() - In corespondance to new!
    /**
     * Beendet die aktuelle WebDriver BiDi Session.
     */
    public EmptyResult endSession() {
        return communicationManager.sendAndWaitForResponse(new SessionRequest.End(), EmptyResult.class);
    }


    /**
     * Abonniert WebDriver BiDi Events.
     * Falls bereits abonniert, wird das Event nicht erneut angefordert.
     */
    public SessionResult.SubscribeResult subscribe(List<String> events) {
        SessionResult.SubscribeResult result = null;
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
            result = communicationManager.sendAndWaitForResponse(
                    new SessionRequest.Subscribe(newEvents), SessionResult.SubscribeResult.class);
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
            communicationManager.sendAndWaitForResponse(new SessionRequest.Unsubscribe(eventsToRemove), EmptyResult.class);
            subscribedEvents.removeAll(eventsToRemove);
            System.out.println("Unsubscribed from events: " + eventsToRemove);
        }
    }
}