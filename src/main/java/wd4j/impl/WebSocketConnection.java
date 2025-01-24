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

        webSocketClient = createAndConfigureWebSocketClient(uri);
    }

    private WebSocketClient createAndConfigureWebSocketClient(URI uri) {
        final WebSocketClient webSocketClient;
        webSocketClient = new WebSocketClient(uri) {
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
        return webSocketClient;
    }

    public synchronized int getNextCommandId() {
        return ++commandCounter;
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

    public String send(Command command) {
        int commandId = command.getId();
        JsonObject commandJson = command.toJsonObject(); // Konvertierung in JsonObject
    
        // Command abschicken
        CompletableFuture<String> future = new CompletableFuture<>();
        pendingCommands.put(commandId, future);
        webSocketClient.send(commandJson.toString());
    
        // Antwort blockierend abholen
        try {
            return receive(commandId);
        } catch( InterruptedException e)
        {
            // Todo
        }
        finally {
            // Aufräumen, um Speicherlecks zu vermeiden
            pendingCommands.remove(commandId);
        }
        return null;
    }

    public CompletableFuture<String> sendAsync(Command command) {
        JsonObject commandJson = command.toJsonObject(); // Konvertierung in JsonObject
        CompletableFuture<String> future = new CompletableFuture<>();
        pendingCommands.put(command.getId(), future);
        webSocketClient.send(commandJson.toString()); // Senden als JSON-String
        return future;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private String receive(int commandId) throws InterruptedException {
        // Warten auf die Antwort des spezifischen Kommandos
        CompletableFuture<String> future = pendingCommands.get(commandId);
        if (future == null) {
            throw new IllegalStateException("No pending command found with ID: " + commandId);
        }
    
        try {
            // Timeout von 20 Sekunden für die Antwort
            return future.get(20, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new RuntimeException("Timeout while waiting for response with ID: " + commandId, e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Error while waiting for response", e);
        }
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

    public void close() {
        try {
            // Schließe alle offenen CompletableFuture und benachrichtige über ausstehende Kommandos
            pendingCommands.forEach((id, future) -> future.completeExceptionally(
                    new RuntimeException("Connection closed before receiving response for command ID: " + id))
            );
            pendingCommands.clear();
    
            // Schließe die WebSocket-Verbindung
            if (webSocketClient.isOpen()) {
                webSocketClient.close();
            }
    
            System.out.println("WebSocket connection closed.");
        } catch (Exception e) {
            System.err.println("Error while closing WebSocket connection: " + e.getMessage());
        }
    }
    
}
