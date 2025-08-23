package de.bund.zrb.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.bund.zrb.manager.WDSessionManager;
import de.bund.zrb.command.response.WDSessionResult;
import de.bund.zrb.support.WDEventMapperImpl;
import de.bund.zrb.support.mapping.GsonMapperFactory;
import de.bund.zrb.type.browsingContext.WDBrowsingContext;
import de.bund.zrb.type.session.WDSubscription;
import de.bund.zrb.type.session.WDSubscriptionRequest;
import de.bund.zrb.websocket.WDEventNames;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class WDEventDispatcher {
    private final Gson gson = GsonMapperFactory.getGson(); // ToDo: Maybe removed

    private final Map<String, ConcurrentLinkedQueue<Consumer<Object>>> eventListeners = new ConcurrentHashMap<>();
    private final BiFunction<String, JsonObject, Object> eventMapper;

    public WDEventDispatcher() {
        this.eventMapper = new WDEventMapperImpl();
    }

    public WDEventDispatcher(BiFunction<String, JsonObject, Object> eventMapper) {
        this.eventMapper = eventMapper;
    }

    public void processEvent(JsonObject jsonMessage) {
        if (!jsonMessage.has("method")) {
            System.err.println("[WARN] Event received without method: " + jsonMessage);
            return;
        }

        String eventType = jsonMessage.get("method").getAsString();
        JsonObject params = jsonMessage.has("params") ? jsonMessage.getAsJsonObject("params") : new JsonObject();

        dispatchEvent(eventType, params);
    }

    /**
     * Dispatches an event to all registered listeners. Params are mapped to the corresponding event type, here.
     *
     * @param eventType
     * @param params
     */
    private void dispatchEvent(String eventType, JsonObject params) {
        WDEventNames eventEnum = WDEventNames.fromName(eventType);

        if (eventEnum == null) {
            System.err.println("[WARN] No event mapping found for event: " + eventType);
            return;
        }

        // Nutze mapEvent() für Mapping in die korrekte Impl-Klasse
        Object event = mapEvent(eventType, params);
        // Fallback:
//        event = mapToPlaywrightInterface(eventType, params, event);

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
        // Registriere das Event in WebDriver BiDi und speichere die Subscription-ID
        WDSessionResult.SubscribeResult result = sessionManager.subscribe(subscriptionRequest);
        WDSubscription subscription = (result != null) ? result.getSubscription() : null;

        // Hole oder erzeuge die Liste der Listener für alle Events
        subscriptionRequest.getEvents().forEach(event -> {
            ConcurrentLinkedQueue<Consumer<Object>> listeners = eventListeners.computeIfAbsent(event, k -> new ConcurrentLinkedQueue<>());

            // Listener zur Liste hinzufügen
            listeners.add((Consumer<Object>) listener);
        });

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
        return eventMapper.apply(eventType, json);
    }




}
