package wd4j.impl.playwright;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import wd4j.api.*;
import wd4j.impl.websocket.Command;
import wd4j.impl.manager.SessionManager;
import wd4j.impl.support.EventDispatcher;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class WebSocketImpl implements WebSocket {
    public final HashMap<String, EventDispatcher> dispatchers = new HashMap<>();
    private WebSocketClient webSocketClient;

    private final List<ConcurrentLinkedQueue<Consumer<Object>>> errorListeners = new ArrayList<>();

    private int commandCounter = 0;
    // ToDo: Should be removed to avoid memory leaks, every command should return a completable future. Thus
    //  the caller can decide what to do with the future, when it is set to completed.
    private final ConcurrentHashMap<Integer, CompletableFuture<WebSocketFrame>> pendingCommands = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();

    private boolean isClosed = false;
    private String url;
    private BiConsumer<Integer, String> onClose;
    private Consumer<WebSocketFrame> onFrameReceived;
    private Consumer<WebSocketFrame> onFrameSent;
    private Consumer<String> onSocketError;

    public WebSocketImpl() { }

    public WebSocketImpl(URI uri) {
        createAndConfigureWebSocketClient(uri);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// **WebSocket-Frames Implementierung**
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static class WebSocketFrameImpl implements WebSocketFrame {
        private final byte[] binaryData;
        private final String textData;

        WebSocketFrameImpl(String text) {
            this.textData = text;
            this.binaryData = null;
        }

        WebSocketFrameImpl(byte[] binary) {
            this.binaryData = binary;
            this.textData = null;
        }

        @Override
        public byte[] binary() {
            return binaryData != null ? binaryData : new byte[0];
        }

        @Override
        public String text() {
            return textData != null ? textData : "";
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// **Frame-basierte Kommunikation**
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // ToDo: Has to be genenric, to be able to handle different response types (no JSON Strings)
    // ToDo: Might use waitForFrameReceived instead of future.get()
    //  This method may be moved to SessionManager or BrowserContextManager
    public String sendAndWaitForResponse(Command command) {
        CompletableFuture<WebSocketFrame> future = send(command);

        try {
            WebSocketFrame frame = future.get(20, TimeUnit.SECONDS);
            return frame.text(); // Antwort als String zurückgeben
        } catch (TimeoutException e) {
            throw new RuntimeException("Timeout while waiting for response with ID: " + command.getId(), e);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error while waiting for response", e);
        }
    }

    @Override
    public WebSocketFrame waitForFrameReceived(WaitForFrameReceivedOptions options, Runnable callback) {
        callback.run(); // This is used to send a command to the server in the origin playwrigth implementation!
        try {
            CompletableFuture<WebSocketFrame> future = new CompletableFuture<>();
            return future.get(20, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new RuntimeException("Timeout waiting for frame received", e);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error while waiting for frame received", e);
        }
    }

    @Override
    // ToDo: Diese Methode soll wohl einfach nur darauf warten, dass irgendein Frame eintrifft, ohne dass vorher
    //  ein Command gesendet wurde (daher kein receive im Bezeichner?). Folglich ist das die Playwright-Methode, um mit
    //  Events zu umzugehen.
    public WebSocketFrame waitForFrameSent(WaitForFrameSentOptions options, Runnable callback) {
        // ToDo: Implementierung
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Sends a command to the WebSocket server and returns a future that will be completed when the response is received.
     *
     * @param command The command to send.
     * @return A future that will be completed when the response is received.
     */
    // ToDo: Diese Implementierung sollte auch eine ebene höher angesetzt sein?
    public CompletableFuture<WebSocketFrame> send(Command command) {
        command.setId(getNextCommandId());
        JsonObject commandJson = command.toJson();
        WebSocketFrameImpl frame = new WebSocketFrameImpl(commandJson.toString());

        CompletableFuture<WebSocketFrame> future = new CompletableFuture<>();
        webSocketClient.send(commandJson.toString());

        if (onFrameSent != null) {
            onFrameSent.accept(frame);
        }

        return future;
    }

    private synchronized int getNextCommandId() {
        return ++commandCounter;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// **WebSocket-Verwaltung**
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    void createAndConfigureWebSocketClient(URI uri) {
        this.url = uri.toString();
        webSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                System.out.println("WebSocket connected: " + handshakedata.getHttpStatusMessage());
            }

            @Override
            public void onMessage(String message) {
                try {
                    JsonObject jsonMessage = gson.fromJson(message, JsonObject.class);
                    if (jsonMessage.has("id")) {
                        int id = jsonMessage.get("id").getAsInt();
                        CompletableFuture<WebSocketFrame> future = pendingCommands.remove(id);
                        if (future != null) {
                            future.complete(new WebSocketFrameImpl(message));
                        }
                    } else if("error".equals(jsonMessage.get("type").getAsString())) // Error
                    {   // Notify all error listeners
                        errorListeners.forEach(queue -> {
                            queue.forEach(listener -> listener.accept(jsonMessage));
                        });
                    }
                    else if("event".equals(jsonMessage.get("type").getAsString()))
                    {
                        // Notify every session about its events
                        dispatchers.forEach((sessionId, dispatcher) -> {
                            // ToDo: Filter events by session ID first
                            dispatcher.processEvent(jsonMessage);
                        });
                    }
                    else
                    {
                        System.err.println("[WEBSOCKET] Received unrecognized message: " + message);
                    }
                } catch (Exception e) {
                    if (onSocketError != null) {
                        onSocketError.accept("Error processing WebSocket message: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                isClosed = true;
                System.out.println("WebSocket closed. Code: " + code + ", Reason: " + reason);
                if (onClose != null) {
                    onClose.accept(code, reason);
                }
            }

            @Override
            public void onError(Exception ex) {
                if (onSocketError != null) {
                    onSocketError.accept("WebSocket error occurred: " + ex.getMessage());
                }
            }
        };
    }

    // ToDo: Kann in Configure Methode integriert werden
    public void connect() throws InterruptedException {
        webSocketClient.connectBlocking();
    }

    public void close() {
        isClosed = true;
        pendingCommands.forEach((id, future) -> future.completeExceptionally(new RuntimeException("Connection closed")));
        pendingCommands.clear();
        webSocketClient.close();
        System.out.println("WebSocket connection closed.");
    }

    public boolean isConnected() {
        return webSocketClient.isOpen();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Overridden methods
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onClose(Consumer<WebSocket> handler) {
        this.onClose = (code, reason) -> handler.accept(this);
    }

    @Override
    public void offClose(Consumer<WebSocket> handler) {
        this.onClose = null;
    }

    @Override
    public void onFrameReceived(Consumer<WebSocketFrame> handler) {
        this.onFrameReceived = handler;
    }

    @Override
    public void offFrameReceived(Consumer<WebSocketFrame> handler) {
        this.onFrameReceived = null;
    }

    @Override
    public void onFrameSent(Consumer<WebSocketFrame> handler) {
        this.onFrameSent = handler;
    }

    @Override
    public void offFrameSent(Consumer<WebSocketFrame> handler) {
        this.onFrameSent = null;
    }

    @Override
    public void onSocketError(Consumer<String> handler) {
        this.onSocketError = handler;
    }

    @Override
    public void offSocketError(Consumer<String> handler) {
        this.onSocketError = null;
    }

    @Override
    public boolean isClosed() {
        return isClosed;
    }

    @Override
    public String url() {
        return url;
    }
}
