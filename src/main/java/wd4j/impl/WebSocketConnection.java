package wd4j.impl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import wd4j.impl.generic.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.net.URI;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

public class WebSocketConnection {
    private final WebSocketClient webSocketClient;
    private final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>(); // Für Events ohne ID
    private BiConsumer<Integer, String> onClose;

    private final ConcurrentHashMap<Integer, CompletableFuture<String>> pendingCommands = new ConcurrentHashMap<>();
    private int commandCounter = 0;

    private final List<Consumer<Event>> eventListeners = new ArrayList<>();

    public WebSocketConnection(URI uri) {
        this.webSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                System.out.println("WebSocket connected: " + handshakedata.getHttpStatusMessage());
            }

            @Override
            public void onMessage(String message) {
                try {
                    System.out.println("Received message: " + message);
                    JsonObject jsonMessage = new Gson().fromJson(message, JsonObject.class);
                    if (jsonMessage.has("id")) {
                        // Antwort auf ein Kommando
                        int id = jsonMessage.get("id").getAsInt();
                        CompletableFuture<String> future = pendingCommands.remove(id);
                        if (future != null) {
                            future.complete(message);
                        }
                    } else if (jsonMessage.has("type")) {
                        // Event verarbeiten
                        String eventType = jsonMessage.get("type").getAsString();
                        JsonObject eventData = jsonMessage.getAsJsonObject("data");
                        Event event = new Event.WebDriverEvent(eventType, eventData);
                        notifyEventListeners(event);
                    } else {
                        // ToDo: Check this! Possible overflow in pendingCommands!
                        messageQueue.put(message); // Unerwartete Nachrichten
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("Error processing message: " + e.getMessage());
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                System.out.println("WebSocketConnection closed. Code: " + code + ", Reason: " + reason);
                if (onClose != null) {
                    onClose.accept(code, reason);
                }
            }

            @Override
            public void onError(Exception ex) {
                System.err.println("WebSocket error occurred:");
                ex.printStackTrace();
            }
        };
    }

    public synchronized int getNextCommandId() {
        return ++commandCounter;
    }

    public CompletableFuture<String> sendAsync(JsonObject command) {
        int id = getNextCommandId();
        command.addProperty("id", id);
        CompletableFuture<String> future = new CompletableFuture<>();
        pendingCommands.put(id, future);
        webSocketClient.send(command.toString());
        return future;
    }

    public void addEventListener(Consumer<Event> listener) {
        eventListeners.add(listener);
    }
    
    private void notifyEventListeners(Event event) {
        for (Consumer<Event> listener : eventListeners) {
            try {
                listener.accept(event);
            } catch (Exception e) {
                System.err.println("Error in event listener: " + e.getMessage());
            }
        }
    }

    public void connect() throws InterruptedException {
        webSocketClient.connectBlocking();
    }

    public void send(String message) {
        System.out.println("Sending message: " + message);
        webSocketClient.send(message);
    }

    public String receive() throws InterruptedException {
        // Set a timeout for receiving messages
        String message = messageQueue.poll(10, TimeUnit.SECONDS);
        if (message == null) {
            throw new RuntimeException("Timeout while waiting for a WebSocket response");
        }
        return message;
    }

    public void close() {
        webSocketClient.close();
    }

    /**
     * Set a consumer to be called when the WebSocket connection is closed (Callback).
     *
     * @param onClose Consumer to be called when the WebSocket connection is closed
     */
    public void setOnClose(BiConsumer<Integer, String> onClose) {
        this.onClose = onClose;
    }

    public WebSocketClient getClient()
    {
        return webSocketClient;
    }
}
