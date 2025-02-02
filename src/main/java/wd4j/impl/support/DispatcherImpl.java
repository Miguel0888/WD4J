package wd4j.impl.support;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import wd4j.api.ConsoleMessage;
import wd4j.api.Request;
import wd4j.api.Response;
import wd4j.core.Dispatcher;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class DispatcherImpl implements Dispatcher {
    private final Gson gson = new Gson();

    // Event-Typen von WebDriver BiDi als Schlüssel verwenden
    private final Map<String, Consumer<JsonObject>> eventHandlers = new ConcurrentHashMap<>();
    private final Map<String, ConcurrentLinkedQueue<Consumer<Object>>> eventListeners = new ConcurrentHashMap<>();

    public DispatcherImpl() {
        // 🔹 WebDriver BiDi Event-Typen zu passenden Methoden mappen
        eventHandlers.put("log.entryAdded", json -> dispatchEvent("log.entryAdded", json, ConsoleMessage.class));
        eventHandlers.put("network.beforeRequestSent", json -> dispatchEvent("network.beforeRequestSent", json, Request.class));
        eventHandlers.put("network.responseStarted", json -> dispatchEvent("network.responseStarted", json, Response.class));
        eventHandlers.put("network.responseCompleted", json -> dispatchEvent("network.responseCompleted", json, Response.class));
        eventHandlers.put("network.requestFailed", json -> dispatchEvent("network.requestFailed", json, Request.class));
    }

    @Override
    public void processEvent(String eventMessage) {
        JsonObject jsonMessage = gson.fromJson(eventMessage, JsonObject.class);

        if (!jsonMessage.has("method")) {
            System.err.println("[WARN] Event received without method: " + jsonMessage);
            return;
        }

        String eventType = jsonMessage.get("method").getAsString();
        JsonObject params = jsonMessage.has("params") ? jsonMessage.getAsJsonObject("params") : new JsonObject();

        // 🔹 Falls ein passender Event-Handler existiert, rufen wir ihn auf
        Consumer<JsonObject> handler = eventHandlers.get(eventType);
        if (handler != null) {
            handler.accept(params);
        } else {
            System.out.println("[INFO] Unrecognized event: " + eventType);
        }
    }

    private <T> void dispatchEvent(String eventType, JsonObject json, Class<T> eventTypeClass) {
        T event = JsonToPlaywrightMapper.mapToInterface(json, eventTypeClass);
        if (eventListeners.containsKey(eventType)) {
            for (Consumer<Object> listener : eventListeners.get(eventType)) {
                listener.accept(event);
            }
        }
    }

    @Override
    public void processResponse(String message) {
        JsonObject jsonMessage = gson.fromJson(message, JsonObject.class);
        System.out.println("[DEBUG] Received response: " + jsonMessage);
    }

    @Override
    public <T> void addEventListener(String eventType, Consumer<T> listener, Class<T> eventTypeClass) {
        eventListeners.computeIfAbsent(eventType, k -> new ConcurrentLinkedQueue<>()).add((Consumer<Object>) listener);
    }

    @Override
    public <T> void removeEventListener(String eventType, Consumer<T> listener) {
        if (eventListeners.containsKey(eventType)) {
            eventListeners.get(eventType).remove(listener);
            if (eventListeners.get(eventType).isEmpty()) {
                eventListeners.remove(eventType); // Löscht die Queue, wenn sie leer ist
            }
        }
    }
}
