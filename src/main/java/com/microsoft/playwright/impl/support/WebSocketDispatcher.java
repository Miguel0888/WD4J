package com.microsoft.playwright.impl.support;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import wd4j.core.Dispatcher;
import wd4j.impl.module.event.NetworkEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class WebSocketDispatcher implements Dispatcher {
    private final Gson gson = new Gson();

    // Event listeners for different WebDriver BiDi events
    private final Map<String, ConcurrentLinkedQueue<Consumer<Object>>> eventListeners = new ConcurrentHashMap<>();

    @Override
    public void processEvent(String eventMessage) {
        JsonElement messageElement = gson.fromJson(eventMessage, JsonElement.class);

        if (!messageElement.isJsonObject()) {
            System.err.println("[WARN] Ignoring event: Expected JsonObject but got " + messageElement);
            return;
        }

        JsonObject jsonMessage = messageElement.getAsJsonObject();

        if (!jsonMessage.has("method")) {
            System.err.println("[WARN] Event received without method: " + jsonMessage);
            return;
        }

        String eventType = jsonMessage.get("method").getAsString();
        JsonObject params = jsonMessage.has("params") ? jsonMessage.getAsJsonObject("params") : new JsonObject();

        Object mappedEvent;
        if ("network.responseStarted".equals(eventType)) {
            mappedEvent = JsonToPlaywrightMapper.mapToInterface(params, NetworkEvent.ResponseStarted.class);
        } else if ("network.responseCompleted".equals(eventType)) {
            mappedEvent = JsonToPlaywrightMapper.mapToInterface(params, NetworkEvent.ResponseCompleted.class);
        } else {
            mappedEvent = params;  // Standardmäßig bleibt es ein JsonObject
        }

        if (eventListeners.containsKey(eventType)) {
            for (Consumer<Object> listener : eventListeners.get(eventType)) {
                listener.accept(mappedEvent);
            }
        }
    }

    @Override
    public void processResponse(String responseMessage) {
        JsonObject jsonMessage = gson.fromJson(responseMessage, JsonObject.class);
        // Handle WebDriver BiDi responses if needed
    }

    @Override
    public <T> void addEventListener(String eventType, Consumer<T> listener, Class<T> eventTypeClass) {
        eventListeners.computeIfAbsent(eventType, k -> new ConcurrentLinkedQueue<>()).add((Consumer<Object>) listener);
    }
}
