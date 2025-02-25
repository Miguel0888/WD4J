package wd4j.impl.support;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import wd4j.api.ConsoleMessage;
import wd4j.api.Frame;
import wd4j.api.Request;
import wd4j.api.Response;
import wd4j.impl.manager.WDSessionManager;
import wd4j.impl.playwright.PageImpl;
import wd4j.impl.playwright.WebSocketImpl;
import wd4j.impl.playwright.event.*;
import wd4j.impl.webdriver.command.response.WDSessionResult;
import wd4j.impl.webdriver.event.WDBrowsingContextEvent;
import wd4j.impl.webdriver.event.WDEventMapping;
import wd4j.impl.webdriver.event.WDLogEvent;
import wd4j.impl.webdriver.event.WDNetworkEvent;
import wd4j.impl.webdriver.mapping.GsonMapperFactory;
import wd4j.impl.webdriver.type.browsingContext.WDBrowsingContext;
import wd4j.impl.webdriver.type.session.WDSubscription;
import wd4j.impl.webdriver.type.session.WDSubscriptionRequest;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class EventDispatcher {
    private final Gson gson = GsonMapperFactory.getGson(); // ToDo: Maybe removed

    // Event-Typen von WebDriver BiDi als Schlüssel verwenden
    private final Map<String, Consumer<JsonObject>> eventHandlers = new ConcurrentHashMap<>();
    private final Map<String, ConcurrentLinkedQueue<Consumer<Object>>> eventListeners = new ConcurrentHashMap<>();

    public EventDispatcher() {
        // 🔹 WebDriver BiDi Event-Typen zu passenden Methoden mappen
        eventHandlers.put("log.entryAdded", json -> dispatchEvent("log.entryAdded", json, ConsoleMessage.class));
        eventHandlers.put("network.beforeRequestSent", json -> dispatchEvent("network.beforeRequestSent", json, Request.class));
        eventHandlers.put("network.responseStarted", json -> dispatchEvent("network.responseStarted", json, Response.class));
        eventHandlers.put("network.responseCompleted", json -> dispatchEvent("network.responseCompleted", json, Response.class));
        eventHandlers.put("network.requestFailed", json -> dispatchEvent("network.requestFailed", json, Request.class));
    }

    public void processEvent(JsonObject jsonMessage) {
        if (!jsonMessage.has("method")) {
            System.err.println("[WARN] Event received without method: " + jsonMessage);
            return;
        }

        String eventType = jsonMessage.get("method").getAsString();
        JsonObject params = jsonMessage.has("params") ? jsonMessage.getAsJsonObject("params") : new JsonObject();

        WDEventMapping event = WDEventMapping.fromName(eventType);
        if (event != null) {
            System.out.println("[DEBUG] Dispatched event: " + eventType + " with params: " + params);
            dispatchEvent(eventType, params, event.getAssociatedClass());
        } else {
            System.out.println("[INFO] Unrecognized event: " + eventType);
        }
    }

    /**
     * Dispatches an event to all registered listeners. Params are mapped to the corresponding event type, here.
     *
     * @param eventType
     * @param params
     * @param eventTypeClass
     * @param <T>
     */
    private <T> void dispatchEvent(String eventType, JsonObject params, Class<T> eventTypeClass) {
        System.out.println("[DEBUG] dispatchEvent() aufgerufen (EventDispatcher Instanz: " + this + ") für eventType=" + eventType);

        System.out.println("[DEBUG] Dispatching event '" + eventType + "' to class: " + eventTypeClass);

        if (eventTypeClass == null) {
            System.err.println("[WARN] No associated class for event: " + eventType);
            return;
        }

        // Nutze mapEvent() für Mapping in die korrekte Impl-Klasse
        Object event = mapEvent(eventType, params);

        // Falls kein Mapping in eine Impl-Klasse möglich war, nutze die alte Mapper-Methode
        if (event == null) {
            event = JsonToPlaywrightMapper.mapToInterface(params, eventTypeClass);
        }

        if (eventListeners.containsKey(eventType)) {
            for (Consumer<Object> listener : eventListeners.get(eventType)) {
                System.out.println("[DEBUG] Calling listener for event: " + eventType);
                listener.accept(event);
            }
        } else {
            System.out.println("[INFO] No listener registered for event: " + eventType);
        }
    }

    public <T> WDSubscription addEventListener(WDSubscriptionRequest subscriptionRequest, Consumer<T> listener, WDSessionManager sessionManager) {
        // Hole oder erzeuge die Liste der Listener für das Event
        ConcurrentLinkedQueue<Consumer<Object>> listeners = eventListeners.computeIfAbsent(subscriptionRequest.getEvents().get(0), k -> {
            return new ConcurrentLinkedQueue<>();
        });

        // Registriere das Event in WebDriver BiDi und speichere die Subscription-ID
        WDSessionResult.SubscribeSessionResult result = sessionManager.subscribe(subscriptionRequest);
        WDSubscription subscription = (result != null) ? result.getSubscription() : null;

        // Listener zur Liste hinzufügen
        listeners.add((Consumer<Object>) listener);

        return subscription;
    }

    @Deprecated // Since the Class is derived from the JSON response via "type"
    public <T> WDSubscription addEventListener(WDSubscriptionRequest subscriptionRequest, Consumer<T> listener, Class<T> eventTypeClass, WDSessionManager sessionManager) {
        // Hole oder erzeuge die Liste der Listener für das Event
        ConcurrentLinkedQueue<Consumer<Object>> listeners = eventListeners.computeIfAbsent(subscriptionRequest.getEvents().get(0), k -> {
            return new ConcurrentLinkedQueue<>();
        });

        // Registriere das Event in WebDriver BiDi und speichere die Subscription-ID
        WDSessionResult.SubscribeSessionResult result = sessionManager.subscribe(subscriptionRequest);
        WDSubscription subscription = (result != null) ? result.getSubscription() : null;

        // Listener zur Liste hinzufügen
        listeners.add((Consumer<Object>) listener);

        return subscription;
    }

    public <T> void removeEventListener(String eventType, Consumer<T> listener, WDSessionManager sessionManager) {
        removeEventListener(eventType, null, listener, sessionManager);
    }

    public <T> void removeEventListener(String eventType, String browsingContextId, Consumer<T> listener, WDSessionManager sessionManager) {
        if (eventListeners.containsKey(eventType)) {
            eventListeners.get(eventType).remove(listener);
            if (eventListeners.get(eventType).isEmpty()) {
                WDBrowsingContext browsingContext = (browsingContextId != null) ? new WDBrowsingContext(browsingContextId) : null;
                // 🛑 Letzter Listener wurde entfernt → WebDriver BiDi Unsubscribe senden
                sessionManager.unsubscribe(Collections.singletonList(eventType), browsingContext == null ? null : Collections.singletonList(browsingContext));
                eventListeners.remove(eventType);
            }
        }
    }

    // ToDo: Not supported yet
    public <T> void removeEventListener(WDSubscription subscription, Consumer<T> listener, WDSessionManager sessionManager) {
        if (subscription == null || listener == null) {
            throw new IllegalArgumentException("Subscription and listener must not be null.");
        }

        sessionManager.unsubscribe(subscription);

        // 🔹 Entferne den Listener aus eventListeners
        eventListeners.values().forEach(listeners -> listeners.remove(listener));

        System.out.println("[INFO] Removed listener for Subscription-ID: " + subscription.value());
    }


    public Object mapEvent(String eventType, JsonObject json) {
        switch (eventType) {
            // ✅ Console-Events
            case "log.entryAdded":
                return new ConsoleMessageImpl(new WDLogEvent.EntryAdded(json));

            // ✅ Netzwerk-Events
            case "network.responseStarted":
                return new ResponseImpl(new WDNetworkEvent.ResponseStarted(json), null);
            case "network.requestWillBeSent":
                return new RequestImpl(new WDNetworkEvent.RequestWillBeSent(json));
            case "network.requestFailed":
                return new RequestFailedImpl(new WDNetworkEvent.RequestFailed(json));
            case "network.requestFinished":
                return new RequestFinishedImpl(new WDNetworkEvent.RequestFinished(json));

            // ✅ BrowsingContext (Seitenbezogene Events)
            case "browsingContext.domContentLoaded":
                return new PageImpl(new WDBrowsingContextEvent(json));
            case "browsingContext.load":
                return new PageImpl(new WDBrowsingContextEvent(json)); // Page Load Event
            case "browsingContext.fragmentNavigated":
                return new NavigationImpl(new WDBrowsingContextEvent(json));

            // ✅ WebSocket-Events
            case "network.webSocketCreated":
                return new WebSocketImpl(new WDNetworkEvent.WebSocketCreated(json));
            case "network.webSocketClosed":
                return new WebSocketImpl(new WDNetworkEvent.WebSocketClosed(json));
            case "network.webSocketFrameSent":
                return new WebSocketImpl.WebSocketFrameImpl(new WDNetworkEvent.WebSocketFrameSent(json));
            case "network.webSocketFrameReceived":
                return new WebSocketImpl.WebSocketFrameImpl(new WDNetworkEvent.WebSocketFrameReceived(json));

            // ✅ Dialoge (Alerts, Prompts, Confirms)
            case "browsingContext.dialogOpened":
                return new DialogImpl(new WDDialogEvent.DialogOpened(json));
            case "browsingContext.dialogClosed":
                return new DialogImpl(new WDDialogEvent.DialogClosed(json));

            // ✅ Web Worker Events
            case "browsingContext.workerStarted":
                return new WorkerImpl(new WDWorkerEvent.WorkerStarted(json));
            case "browsingContext.workerTerminated":
                return new WorkerImpl(new WDWorkerEvent.WorkerTerminated(json));

            // ✅ Page Events
            case "browsingContext.pageCrashed":
                return new PageImpl(new WDBrowsingContextEvent(json));
            case "browsingContext.pageClosed":
                return new PageImpl(new WDBrowsingContextEvent(json));

            // ✅ Sonstige Playwright Events
            case "browsingContext.popupOpened":
                return new PageImpl(new WDBrowsingContextEvent(json)); // Playwright's Popup Event
            case "network.certificateError":
                return new SecurityDetailsImpl(new WDSecurityEvent.CertificateError(json));

            default:
                return null;
        }
    }


}
