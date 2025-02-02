package wd4j.impl.support;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import wd4j.api.ConsoleMessage;
import wd4j.api.Request;
import wd4j.api.Response;
import wd4j.api.WebSocket;
import wd4j.core.Dispatcher;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class DispatcherImpl implements Dispatcher {
    private final Gson gson = new Gson();
    private final Map<String, Consumer<JsonObject>> eventHandlers = new ConcurrentHashMap<>();
    private final Map<String, ConcurrentLinkedQueue<Consumer<Object>>> eventListeners = new ConcurrentHashMap<>();

    public DispatcherImpl() {
        // 🔹 Mapping von Event-Typen zu passenden Methoden
        eventHandlers.put("log.entryAdded", json -> dispatchEvent(json, ConsoleMessage.class));
        eventHandlers.put("network.requestWillBeSent", json -> dispatchEvent(json, Request.class));
        eventHandlers.put("network.responseReceived", json -> dispatchEvent(json, Response.class));
        eventHandlers.put("network.webSocketCreated", json -> dispatchEvent(json, WebSocket.class));
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

    private <T> void dispatchEvent(JsonObject json, Class<T> eventTypeClass) {
        T event = JsonToPlaywrightMapper.mapToInterface(json, eventTypeClass);
        if (eventListeners.containsKey(eventTypeClass.getSimpleName())) {
            for (Consumer<Object> listener : eventListeners.get(eventTypeClass.getSimpleName())) {
                listener.accept(event);
            }
        }
    }

    @Override
    public void processResponse(String message) {
        JsonObject jsonMessage = gson.fromJson(message, JsonObject.class);
        System.out.println("[DEBUG] Received response: " + jsonMessage);
        // Falls in Zukunft eine Response-Verarbeitung nötig wird
    }

    @Override
    public <T> void addEventListener(String eventType, Consumer<T> listener, Class<T> eventTypeClass) {
        eventListeners.computeIfAbsent(eventTypeClass.getSimpleName(), k -> new ConcurrentLinkedQueue<>()).add((Consumer<Object>) listener);
    }
}
