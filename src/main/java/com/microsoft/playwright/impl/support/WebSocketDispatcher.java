package com.microsoft.playwright.impl.support;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.microsoft.playwright.ConsoleMessage;
import com.microsoft.playwright.Response;
import wd4j.core.Dispatcher;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class WebSocketDispatcher implements Dispatcher {
    private final Gson gson = new Gson();

    // Listener-Queues für Events und Responses
    public final ConcurrentLinkedQueue<Consumer<ConsoleMessage>> consoleMessageListeners = new ConcurrentLinkedQueue<>();
    public final ConcurrentLinkedQueue<Consumer<Response>> responseListeners = new ConcurrentLinkedQueue<>();

    @Override
    public void processEvent(String eventMessage) {
        JsonObject jsonMessage = gson.fromJson(eventMessage, JsonObject.class);
        ConsoleMessage message = JsonToPlaywrightMapper.mapToInterface(jsonMessage, ConsoleMessage.class);

        if (message != null) {
            for (Consumer<ConsoleMessage> listener : consoleMessageListeners) {
                listener.accept(message);
            }
        }
    }

    @Override
    public void processResponse(String responseMessage) {
        JsonObject jsonMessage = gson.fromJson(responseMessage, JsonObject.class);
        Response response = JsonToPlaywrightMapper.mapToInterface(jsonMessage, Response.class);

        if (response != null) {
            for (Consumer<Response> listener : responseListeners) {
                listener.accept(response);
            }
        }
    }

}
