package de.bund.zrb.chrome.cdp;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Low-level Chrome DevTools Protocol (CDP) connection over WebSocket.
 * Handles request/response matching via command IDs and event dispatching.
 */
public class CdpConnection {
    private static final Gson GSON = new Gson();
    private final AtomicInteger nextId = new AtomicInteger(1);
    private final ConcurrentHashMap<Integer, CompletableFuture<JsonObject>> pendingCommands = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<Consumer<JsonObject>>> eventListeners = new ConcurrentHashMap<>();
    private final WebSocketClient wsClient;
    private volatile boolean closed = false;

    public CdpConnection(URI cdpEndpoint) throws InterruptedException {
        this.wsClient = new WebSocketClient(cdpEndpoint) {
            @Override
            public void onOpen(ServerHandshake handshake) {
                System.out.println("[CDP] Connected to " + cdpEndpoint);
            }

            @Override
            public void onMessage(String message) {
                handleMessage(message);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                closed = true;
                System.out.println("[CDP] WebSocket closed: " + code + " " + reason);
                // Fail all pending commands
                for (Map.Entry<Integer, CompletableFuture<JsonObject>> entry : pendingCommands.entrySet()) {
                    entry.getValue().completeExceptionally(new RuntimeException("CDP connection closed"));
                }
                pendingCommands.clear();
            }

            @Override
            public void onError(Exception ex) {
                System.err.println("[CDP] WebSocket error: " + ex.getMessage());
            }
        };
        wsClient.setConnectionLostTimeout(60);
        wsClient.connectBlocking(10, TimeUnit.SECONDS);
    }

    /**
     * Send a CDP command without sessionId.
     */
    public CompletableFuture<JsonObject> sendCommand(String method, JsonObject params) {
        return sendCommand(method, params, null);
    }

    /**
     * Send a CDP command with optional sessionId (for session-scoped commands).
     */
    public CompletableFuture<JsonObject> sendCommand(String method, JsonObject params, String sessionId) {
        if (closed) {
            CompletableFuture<JsonObject> f = new CompletableFuture<>();
            f.completeExceptionally(new RuntimeException("CDP connection is closed"));
            return f;
        }

        int id = nextId.getAndIncrement();
        JsonObject request = new JsonObject();
        request.addProperty("id", id);
        request.addProperty("method", method);
        if (params != null) {
            request.add("params", params);
        }
        if (sessionId != null) {
            request.addProperty("sessionId", sessionId);
        }

        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        pendingCommands.put(id, future);

        String json = GSON.toJson(request);
        if (Boolean.getBoolean("wd4j.log.cdp")) {
            System.out.println("[CDP] >>> " + json);
        }
        wsClient.send(json);

        return future;
    }

    /**
     * Register a listener for a specific CDP event method (e.g. "Runtime.bindingCalled").
     */
    public void addEventListener(String method, Consumer<JsonObject> listener) {
        eventListeners.computeIfAbsent(method, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    /**
     * Remove a specific event listener.
     */
    public void removeEventListener(String method, Consumer<JsonObject> listener) {
        List<Consumer<JsonObject>> listeners = eventListeners.get(method);
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    /**
     * Close the CDP connection.
     */
    public void close() {
        closed = true;
        wsClient.close();
    }

    public boolean isOpen() {
        return wsClient.isOpen() && !closed;
    }

    // ── Internal ──

    private void handleMessage(String message) {
        if (Boolean.getBoolean("wd4j.log.cdp")) {
            // Truncate long messages for logging
            String logMsg = message.length() > 500 ? message.substring(0, 500) + "..." : message;
            System.out.println("[CDP] <<< " + logMsg);
        }

        try {
            JsonObject json = JsonParser.parseString(message).getAsJsonObject();

            // Response to a command?
            if (json.has("id")) {
                int id = json.get("id").getAsInt();
                CompletableFuture<JsonObject> future = pendingCommands.remove(id);
                if (future != null) {
                    if (json.has("error")) {
                        JsonObject error = json.getAsJsonObject("error");
                        String errorMsg = error.has("message") ? error.get("message").getAsString() : "Unknown CDP error";
                        future.completeExceptionally(new RuntimeException("CDP error: " + errorMsg));
                    } else {
                        JsonObject result = json.has("result") ? json.getAsJsonObject("result") : new JsonObject();
                        future.complete(result);
                    }
                }
                return;
            }

            // Event?
            if (json.has("method")) {
                String method = json.get("method").getAsString();
                JsonObject params = json.has("params") ? json.getAsJsonObject("params") : new JsonObject();
                List<Consumer<JsonObject>> listeners = eventListeners.get(method);
                if (listeners != null) {
                    for (Consumer<JsonObject> listener : listeners) {
                        try {
                            listener.accept(params);
                        } catch (Exception e) {
                            System.err.println("[CDP] Error in event listener for " + method + ": " + e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[CDP] Error parsing message: " + e.getMessage());
        }
    }
}

