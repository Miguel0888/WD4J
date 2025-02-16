package wd4j.impl.support;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import wd4j.api.ConsoleMessage;
import wd4j.api.Request;
import wd4j.api.Response;
import wd4j.impl.manager.WDSessionManager;
import wd4j.impl.webdriver.event.WDEventMapping;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class EventDispatcher {
    private final Gson gson = new Gson();

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
     * @param json
     * @param eventTypeClass
     * @param <T>
     */
    private <T> void dispatchEvent(String eventType, JsonObject params, Class<T> eventTypeClass) {
        System.out.println("[DEBUG] Dispatching event '" + eventType + "' to class: " + eventTypeClass);

        if (eventTypeClass == null) {
            System.err.println("[WARN] No associated class for event: " + eventType);
            return;
        }

        // Konvertiere JSON in Playwright-Objekt
        T event = JsonToPlaywrightMapper.mapToInterface(params, eventTypeClass); // public fields may cause problems though

        if (event == null) {
            System.err.println("[ERROR] Mapping failed for event: " + eventType);
            return;
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

    public <T> void addEventListener(String eventType, Consumer<T> listener, Class<T> eventTypeClass, WDSessionManager WDSessionManager) {
        eventListeners.computeIfAbsent(eventType, k -> {
            // 🚀 Erster Listener für dieses Event → WebDriver BiDi Subscribe senden
            WDSessionManager.subscribe(Collections.singletonList(eventType));
            return new ConcurrentLinkedQueue<>();
        }).add((Consumer<Object>) listener);
    }

    public <T> void removeEventListener(String eventType, Consumer<T> listener, WDSessionManager WDSessionManager) {
        if (eventListeners.containsKey(eventType)) {
            eventListeners.get(eventType).remove(listener);
            if (eventListeners.get(eventType).isEmpty()) {
                // 🛑 Letzter Listener wurde entfernt → WebDriver BiDi Unsubscribe senden
                WDSessionManager.unsubscribe(Collections.singletonList(eventType));
                eventListeners.remove(eventType);
            }
        }
    }
}
