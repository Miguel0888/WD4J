package wd4j.impl.manager;

import wd4j.impl.markerInterfaces.WDModule;
import wd4j.impl.dto.command.request.WDSessionRequest;
import wd4j.impl.dto.command.request.parameters.session.parameters.UnsubscribeParameters;
import wd4j.impl.dto.command.response.WDEmptyResult;
import wd4j.impl.dto.command.response.WDSessionResult;
import wd4j.impl.dto.type.browsingContext.WDBrowsingContext;
import wd4j.impl.dto.type.session.WDSubscription;
import wd4j.impl.dto.type.session.WDSubscriptionRequest;
import wd4j.impl.websocket.WebSocketManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

public class WDSessionManager implements WDModule {
    private final WebSocketManager webSocketManager;
    private final Map<WDSubscriptionRequest, String> subscriptionIds = new ConcurrentHashMap<>();
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
    public WDSessionResult.StatusResult status() {
        return webSocketManager.sendAndWaitForResponse(new WDSessionRequest.Status(), WDSessionResult.StatusResult.class);
    }

    // new() - Since plain "new" is a reserved word in Java!
    /**
     * Erstellt eine neue Session mit dem gegebenen Browser.
     */
    public WDSessionResult.NewResult newSession(String browserName) {
        return webSocketManager.sendAndWaitForResponse(new WDSessionRequest.New(browserName), WDSessionResult.NewResult.class);
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
    public WDSessionResult.SubscribeResult subscribe(WDSubscriptionRequest subscriptionRequest) {
        if (subscriptionRequest == null || subscriptionRequest.getEvents().isEmpty()) {
            throw new IllegalArgumentException("Subscription request must not be null or empty.");
        }

        // Prüfe, ob genau diese Subscription bereits existiert
        if (subscriptionIds.containsKey(subscriptionRequest)) {
            System.out.println("Subscription already exists for: " + subscriptionRequest);
            return null;
        }

        // Erzeuge das Command-Objekt mit der ID
        WDSessionRequest.Subscribe subscribeCommand = new WDSessionRequest.Subscribe(subscriptionRequest);

        // Die ID ist bereits bekannt, sollt aber eigentlich in der Antwort geliefert werden (ToDo: Wait for fix in WebDriver BiDi!)
        String subscriptionId = subscribeCommand.getId().toString();

        // Sende den Subscribe-Command
        WDSessionResult.SubscribeResult result = webSocketManager.sendAndWaitForResponse(
                subscribeCommand, WDSessionResult.SubscribeResult.class);

        // Bug fix: Falls die Antwort leer ist, erstelle ein Fallback-Result mit der ID
        if (result == null || result.getSubscription() == null) {
            System.out.println("Warning: No Subscription-ID returned. Using command ID as fallback.");
            result = new WDSessionResult.SubscribeResult(new WDSubscription(subscriptionId));
        }
        // End Bug fix (ToDo: Remove when fixed in WebDriver BiDi!)

        // Speichere die Subscription mit ihren vollen Kriterien
        subscriptionIds.put(subscriptionRequest, subscriptionId);

        System.out.println("Subscribed to events: " + subscriptionRequest.getEvents() + " with Subscription-ID: " + subscriptionId);

        return result;
    }

    /**
     * Entfernt die Event-Subscription für WebDriver BiDi Events.
     */
    public void unsubscribe(List<String> events, List<WDBrowsingContext> contexts) {
        if (events == null || events.isEmpty()) {
            throw new IllegalArgumentException("Events list must not be null or empty.");
        }

        List<String> eventsToRemove = new ArrayList<>();
        for (String event : events) {
            // ToDo: Fix this. Es gibt jetzt zwei Listen mit Events: subscribedEvents und subscriptionIds
//            if (subscribedEvents.contains(event)) {
                eventsToRemove.add(event);
//            }
        }

        // Baue die Unsubscribe-Parameter
        UnsubscribeParameters unsubscribeParameters =
                new UnsubscribeParameters.WDUnsubscribeByAttributesRequestParams(eventsToRemove, contexts);

        if (!eventsToRemove.isEmpty()) {
            // 🔹 Unsubscribe-Request mit Events und Contexts senden
            WDSessionRequest.Unsubscribe unsubscribeRequest = new WDSessionRequest.Unsubscribe(unsubscribeParameters);

            webSocketManager.sendAndWaitForResponse(unsubscribeRequest, WDEmptyResult.class);
            subscribedEvents.removeAll(eventsToRemove);

            System.out.println("[INFO] Unsubscribed from events: " + eventsToRemove + " for contexts: " + contexts);
        }
    }


    // ToDo: Not Supported Yet!?!
    // [WebSocket] Message sent: {"id":6,"method":"session.unsubscribe","params":{"subscriptions":["5"]}}
    //[WebSocket] Message received: {"type":"error","id":6,"error":"invalid argument","message":"Expected \"events\" to be an array, got [object Undefined] undefined","stacktrace":"RemoteError@chrome://remote/content/shared/RemoteError.sys.mjs:8:8\nWebDriverError@chrome://remote/content/shared/webdriver/Errors.sys.mjs:197:5\nInvalidArgumentError@chrome://remote/content/shared/webdriver/Errors.sys.mjs:392:5\nassert.that/<@chrome://remote/content/shared/webdriver/Assert.sys.mjs:538:13\nassert.array@chrome://remote/content/shared/webdriver/Assert.sys.mjs:511:41\n#assertNonEmptyArrayWithStrings@chrome://remote/content/webdriver-bidi/modules/root/session.sys.mjs:136:17\nunsubscribe@chrome://remote/content/webdriver-bidi/modules/root/session.sys.mjs:115:41\nhandleCommand@chrome://remote/content/shared/messagehandler/MessageHandler.sys.mjs:257:33\nexecute@chrome://remote/content/shared/webdriver/Session.sys.mjs:390:32\nonPacket@chrome://remote/content/webdriver-bidi/WebDriverBiDiConnection.sys.mjs:236:37\nonMessage@chrome://remote/content/server/WebSocketTransport.sys.mjs:127:18\nhandleEvent@chrome://remote/content/server/WebSocketTransport.sys.mjs:109:14\n"}
    public void unsubscribe(WDSubscription subscription) {
        if (subscription == null || subscription.value().isEmpty()) {
            throw new IllegalArgumentException("Subscription ID must not be null or empty.");
        }

        // Baue die Unsubscribe-Parameter
        UnsubscribeParameters unsubscribeParameters =
                new UnsubscribeParameters.WDUnsubscribeByIDRequestParams(Collections.singletonList(subscription));

        // Sende den Unsubscribe-Command mit der Subscription-ID
        webSocketManager.sendAndWaitForResponse(new WDSessionRequest.Unsubscribe(unsubscribeParameters), WDEmptyResult.class);

        // Entferne die Subscription aus der Map (Vergleich als String)
        subscriptionIds.entrySet().removeIf(entry -> entry.getValue().equals(subscription.value()));

        System.out.println("Unsubscribed from event using Subscription-ID: " + subscription.value());
    }

    /**
     * Entfernt alle aktiven Event-Subscriptions.
     */
    @Deprecated
    public void unsubscribeAll() {
        if (subscriptionIds.isEmpty()) {
            System.out.println("No active subscriptions to remove.");
            return;
        }

        // Erstelle eine Liste mit allen Subscription-IDs
        List<WDSubscription> subscriptionsToRemove = new ArrayList<>();
        for (String subscriptionId : subscriptionIds.values()) {
            subscriptionsToRemove.add(new WDSubscription(subscriptionId));
        }

        // Baue die Unsubscribe-Parameter
        UnsubscribeParameters unsubscribeParameters =
                new UnsubscribeParameters.WDUnsubscribeByIDRequestParams(subscriptionsToRemove);

        // Sende den Unsubscribe-Command für alle aktiven Subscriptions
        webSocketManager.sendAndWaitForResponse(new WDSessionRequest.Unsubscribe(unsubscribeParameters), WDEmptyResult.class);

        // Leere die Subscription-Map
        subscriptionIds.clear();

        System.out.println("Unsubscribed from all events.");
    }
}