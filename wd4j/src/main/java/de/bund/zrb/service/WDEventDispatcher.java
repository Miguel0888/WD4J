package de.bund.zrb.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.bund.zrb.command.response.WDSessionResult;
import de.bund.zrb.manager.WDSessionManager;
import de.bund.zrb.support.WDEventMapperImpl;
import de.bund.zrb.support.mapping.GsonMapperFactory;
import de.bund.zrb.type.browsingContext.WDBrowsingContext;
import de.bund.zrb.type.session.WDSubscription;
import de.bund.zrb.type.session.WDSubscriptionRequest;
import de.bund.zrb.websocket.WDEventNames;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static java.lang.Thread.sleep;

/**
 * Low-level dispatcher for WebDriver BiDi events.
 * Keep type-based fan-out and add optional context-based fan-out.
 */
public class WDEventDispatcher {
    private final Gson gson = GsonMapperFactory.getGson(); // ToDo: Maybe removed
    private final BiFunction<String, JsonObject, Object> eventMapper;

    // Global listeners per event type (unchanged behavior, but key now is enum)
    private final Map<WDEventNames, ConcurrentLinkedQueue<Consumer<Object>>> eventListeners = new ConcurrentHashMap<WDEventNames, ConcurrentLinkedQueue<Consumer<Object>>>();

    // Context-aware listeners: EventType -> (ContextId -> Listeners)
    private final Map<WDEventNames, Map<String, ConcurrentLinkedQueue<Consumer<Object>>>> contextListeners = new ConcurrentHashMap<WDEventNames, Map<String, ConcurrentLinkedQueue<Consumer<Object>>>>();

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
        final String method = jsonMessage.get("method").getAsString();
        final JsonObject params = jsonMessage.has("params") ? jsonMessage.getAsJsonObject("params") : new JsonObject();

        final WDEventNames eventEnum = WDEventNames.fromName(method);
        if (eventEnum == null) {
            System.err.println("[WARN] No event mapping found for event: " + method);
            return;
        }
        dispatchEvent(eventEnum, params);
    }

    /**
     * Dispatches an event to all registered listeners (global and context-aware).
     * Map params to DTO once to keep behavior consistent.
     */
    private void dispatchEvent(WDEventNames eventEnum, JsonObject params) {
        // Map JSON params to typed DTO (unchanged strategy)
        final Object event = mapEvent(eventEnum.getName(), params);

        // Determine browsing context id for context-aware listeners
        final String ctxId = WDEventContextExtractor.extractContextId(eventEnum, params);

        // 1) Fan-out to context-aware listeners (if any)
        final Map<String, ConcurrentLinkedQueue<Consumer<Object>>> byContext = contextListeners.get(eventEnum);
        if (byContext != null && ctxId != null) {
            final ConcurrentLinkedQueue<Consumer<Object>> ctxQueue = byContext.get(ctxId);
            if (ctxQueue != null && !ctxQueue.isEmpty()) {
                for (Consumer<Object> l : ctxQueue) {
                    try {
                        // Comment: Deliver DTO to context-scoped listener
                        l.accept(event);
                    } catch (Throwable t) {
                        // Comment: Do not break dispatch chain
                    }
                }
            }
        }

        // 2) Fan-out to global listeners (unchanged behavior)
        final ConcurrentLinkedQueue<Consumer<Object>> globals = eventListeners.get(eventEnum);
        if (globals != null && !globals.isEmpty()) {
            for (Consumer<Object> listener : globals) {
                try {
                    // Comment: Deliver DTO to global listener
                    listener.accept(event);
                } catch (Throwable t) {
                    // Comment: Do not break dispatch chain
                }
            }
        }

        if ((byContext == null || (ctxId != null && (byContext.get(ctxId) == null || byContext.get(ctxId).isEmpty())))
                && (globals == null || globals.isEmpty())) {
            System.out.println("[INFO] No listener registered for event: " + eventEnum.getName());
        }
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Public API – global (event-typ) Listener (bestehendes Verhalten)
    // -----------------------------------------------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    public <T> WDSubscription addEventListener(WDSubscriptionRequest subscriptionRequest, Consumer<T> listener, WDSessionManager sessionManager) {
        // Keep original behavior: subscribe at BiDi for the given request
        WDSessionResult.SubscribeResult result = sessionManager.subscribe(subscriptionRequest);
        // ToDo: Fix this!
        try {
            sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        WDSubscription subscription = (result != null) ? result.getSubscription() : null;

        // Register listener per event type (enum key)
        for (String eventName : subscriptionRequest.getEvents()) {
            WDEventNames type = WDEventNames.fromName(eventName);
            if (type == null) continue;
            ConcurrentLinkedQueue<Consumer<Object>> q = eventListeners.computeIfAbsent(type, k -> new ConcurrentLinkedQueue<Consumer<Object>>());
            q.add((Consumer<Object>) listener);
        }
        return subscription;
    }

    public <T> void removeEventListener(String eventType, Consumer<T> listener, WDSessionManager sessionManager) {
        removeEventListener(eventType, null, listener, sessionManager);
    }

    public <T> void removeEventListener(String eventType, String browsingContextId, Consumer<T> listener, WDSessionManager sessionManager) {
        // Global removal (ignore context id)
        WDEventNames type = WDEventNames.fromName(eventType);
        if (type == null) return;

        boolean hadAny = false;

        // Remove from global list
        ConcurrentLinkedQueue<Consumer<Object>> globals = eventListeners.get(type);
        if (globals != null) {
            hadAny = globals.remove(listener) || hadAny;
            if (globals.isEmpty()) {
                eventListeners.remove(type);
            }
        }

        // If there are still listeners (global or any context), do not unsubscribe at BiDi
        boolean stillHasListeners = hasAnyListener(type);
        if (!stillHasListeners) {
            // Unsubscribe from BiDi (keep old semantics)
            WDBrowsingContext bc = (browsingContextId != null) ? new WDBrowsingContext(browsingContextId) : null;
            sessionManager.unsubscribe(Collections.singletonList(eventType), bc == null ? null : Collections.singletonList(bc));
        }
    }

    // ToDo: Not supported yet
    public <T> void removeEventListener(WDSubscription subscription, Consumer<T> listener, WDSessionManager sessionManager) {
        if (subscription == null || listener == null) {
            throw new IllegalArgumentException("Subscription and listener must not be null.");
        }

        sessionManager.unsubscribe(subscription);

        // Remove listener from all containers
        for (Map.Entry<WDEventNames, ConcurrentLinkedQueue<Consumer<Object>>> e : eventListeners.entrySet()) {
            e.getValue().remove(listener);
            if (e.getValue().isEmpty()) {
                eventListeners.remove(e.getKey());
            }
        }
        for (Map.Entry<WDEventNames, Map<String, ConcurrentLinkedQueue<Consumer<Object>>>> e : contextListeners.entrySet()) {
            Map<String, ConcurrentLinkedQueue<Consumer<Object>>> m = e.getValue();
            for (Map.Entry<String, ConcurrentLinkedQueue<Consumer<Object>>> c : m.entrySet()) {
                c.getValue().remove(listener);
            }
            // Prune empty maps
            m.values().removeIf(ConcurrentLinkedQueue::isEmpty);
            if (m.isEmpty()) {
                contextListeners.remove(e.getKey());
            }
        }

        System.out.println("[INFO] Removed listener for Subscription-ID: " + subscription.value());
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Public API – neue kontextorientierte Listener
    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Subscribe and listen only for a single browsing context (page/frame).
     * The DTO is delivered to the listener.
     */
    @SuppressWarnings("unchecked")
    public <T> WDSubscription addEventListenerForContext(WDSubscriptionRequest subscriptionRequest,
                                                         String browsingContextId,
                                                         Consumer<T> listener,
                                                         WDSessionManager sessionManager) {
        if (browsingContextId == null) {
            throw new IllegalArgumentException("browsingContextId must not be null");
        }

        // Prefer subscribing at BiDi already filtered by this context (if supported by your WDSessionManager)
        WDSubscriptionRequest ctxScoped = new WDSubscriptionRequest(
                new ArrayList<String>(subscriptionRequest.getEvents()),
                Collections.singletonList(new WDBrowsingContext(browsingContextId))
        );
        WDSessionResult.SubscribeResult result = sessionManager.subscribe(ctxScoped);
        WDSubscription subscription = (result != null) ? result.getSubscription() : null;

        // Register listener per event type inside the context bucket
        for (String eventName : subscriptionRequest.getEvents()) {
            WDEventNames type = WDEventNames.fromName(eventName);
            if (type == null) continue;
            Map<String, ConcurrentLinkedQueue<Consumer<Object>>> byCtx =
                    contextListeners.computeIfAbsent(type, k -> new ConcurrentHashMap<String, ConcurrentLinkedQueue<Consumer<Object>>>());
            ConcurrentLinkedQueue<Consumer<Object>> q =
                    byCtx.computeIfAbsent(browsingContextId, k -> new ConcurrentLinkedQueue<Consumer<Object>>());
            q.add((Consumer<Object>) listener);
        }
        return subscription;
    }

    /**
     * Remove a context-scoped listener. If last listener for event type is gone (both global and all contexts),
     * send BiDi unsubscribe (old semantics preserved).
     */
    public <T> void removeEventListenerForContext(String eventType,
                                                  String browsingContextId,
                                                  Consumer<T> listener,
                                                  WDSessionManager sessionManager) {
        if (eventType == null || browsingContextId == null || listener == null) return;

        WDEventNames type = WDEventNames.fromName(eventType);
        if (type == null) return;

        Map<String, ConcurrentLinkedQueue<Consumer<Object>>> byCtx = contextListeners.get(type);
        if (byCtx != null) {
            ConcurrentLinkedQueue<Consumer<Object>> q = byCtx.get(browsingContextId);
            if (q != null) {
                q.remove(listener);
                if (q.isEmpty()) {
                    byCtx.remove(browsingContextId);
                }
            }
            if (byCtx.isEmpty()) {
                contextListeners.remove(type);
            }
        }

        // Unsubscribe if there is no listener left at all for this event type
        if (!hasAnyListener(type)) {
            WDBrowsingContext bc = new WDBrowsingContext(browsingContextId);
            sessionManager.unsubscribe(Collections.singletonList(eventType), Collections.singletonList(bc));
        }
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------------------------------------------------

    private boolean hasAnyListener(WDEventNames type) {
        ConcurrentLinkedQueue<Consumer<Object>> globals = eventListeners.get(type);
        if (globals != null && !globals.isEmpty()) return true;

        Map<String, ConcurrentLinkedQueue<Consumer<Object>>> byCtx = contextListeners.get(type);
        if (byCtx != null) {
            for (ConcurrentLinkedQueue<Consumer<Object>> q : byCtx.values()) {
                if (q != null && !q.isEmpty()) return true;
            }
        }
        return false;
    }

    public Object mapEvent(String eventType, JsonObject json) {
        return eventMapper.apply(eventType, json);
    }
}
