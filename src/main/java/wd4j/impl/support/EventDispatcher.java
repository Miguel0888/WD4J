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
import wd4j.impl.webdriver.event.*;
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
        WDEventMapping eventMapping = WDEventMapping.fromName(eventType);
        if (eventMapping == null) {
            return null; // Falls das Event nicht bekannt ist, nichts zurückgeben
        }

        switch (eventMapping) {
            // 🔹 Browsing Context Events
            case CONTEXT_CREATED:
                return new PageImpl(new WDBrowsingContextEvent.Created(json));
            case CONTEXT_DESTROYED:
                return new PageImpl(new WDBrowsingContextEvent.Destroyed(json));

            case NAVIGATION_STARTED:
                return new FrameImpl(new WDBrowsingContextEvent.NavigationStarted(json));
            case NAVIGATION_COMMITTED:
                return null; // ToDo: How map onto Playwright?
            case NAVIGATION_FAILED:
                return new FrameImpl(new WDBrowsingContextEvent.NavigationFailed(json));
            case NAVIGATION_ABORTED:
                return new FrameImpl(new WDBrowsingContextEvent.NavigationAborted(json));
            case FRAGMENT_NAVIGATED:
                return new FrameImpl(new WDBrowsingContextEvent.FragmentNavigated(json)); // ToDo: Check if correct
            case HISTORY_UPDATED:
                return new FrameImpl(new WDBrowsingContextEvent.HistoryUodated(json));

            case DOM_CONTENT_LOADED:
                return new PageImpl(new WDBrowsingContextEvent.DomContentLoaded(json));
            case LOAD:
                return new PageImpl(new WDBrowsingContextEvent.Load(json));

            case DOWNLOAD_WILL_BEGIN:
                return new DownloadImpl(new WDBrowsingContextEvent.DownloadWillBegin(json));

            case USER_PROMPT_OPENED:
                return new DialogImpl(new WDBrowsingContextEvent.UserPromptOpened(json));
            case USER_PROMPT_CLOSED:
                return new DialogImpl(new WDBrowsingContextEvent.UserPromptClosed(json));

            // 🔹 Network Events
            case AUTH_REQUIRED:
                return new AuthChallenge(new WDNetworkEvent.AuthRequired(json)); // ToDo: How map onto Playwright?

            case BEFORE_REQUEST_SENT:
                return new RequestImpl(new WDNetworkEvent.BeforeRequestSent(json));

            case FETCH_ERROR:
                return new ResponseImpl(new WDNetworkEvent.FetchError(json), null);

            case RESPONSE_STARTED:
                return new ResponseImpl(new WDNetworkEvent.ResponseStarted(json), null);
            case RESPONSE_COMPLETED:
                return new ResponseImpl(new WDNetworkEvent.ResponseCompleted(json), null);

            // 🔹 WebSocket Events
            case MESSAGE:
                return new ConsoleMessageImpl(new WDLogEvent.EntryAdded(json));

            // 🔹 Script Events
            case REALM_CREATED:
                return new WorkerImpl(new WDScriptEvent.RealmCreated(json));
            case REALM_DESTROYED:
                return new WorkerImpl(new WDScriptEvent.RealmDestroyed(json));

            // 🔹 Log Events
            case ENTRY_ADDED:
                return new ConsoleMessageImpl(new WDLogEvent.EntryAdded(json));

            default:
                return null;
        }
    }




}
