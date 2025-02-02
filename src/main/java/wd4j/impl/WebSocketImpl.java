package wd4j.impl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import wd4j.api.*;
import wd4j.core.Dispatcher;
import wd4j.core.generic.Command;
import wd4j.impl.support.DispatcherImpl;
import wd4j.impl.support.JsonToPlaywrightMapper;

import java.net.URI;
import java.util.concurrent.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class WebSocketImpl implements WebSocket {
    private final Dispatcher dispatcher; // Dispatcher-Objekt, das die Events verarbeitet
    private WebSocketClient webSocketClient;

    private int commandCounter = 0;
    private final ConcurrentHashMap<Integer, CompletableFuture<String>> pendingCommands = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();

    private boolean isClosed = false;
    private String url;
    private BiConsumer<Integer, String> onClose;
    private Consumer<WebSocketFrame> onFrameReceived;
    private Consumer<WebSocketFrame> onFrameSent;
    private Consumer<String> onSocketError;

    public WebSocketImpl() {
        this.dispatcher = new DispatcherImpl();
    }

    public WebSocketImpl(URI uri) {
        this.dispatcher = new DispatcherImpl();
        createAndConfigureWebSocketClient(uri);
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

    @Override
    public WebSocketFrame waitForFrameReceived(WaitForFrameReceivedOptions options, Runnable callback) {
        // ToDo
        return null;
    }

    @Override
    public WebSocketFrame waitForFrameSent(WaitForFrameSentOptions options, Runnable callback) {
        // ToDo
        return null;
    }


    private static class WebSocketFrameImpl implements WebSocketFrame {
        private final byte[] binaryData;
        private final String textData;

        // Konstruktor für Textframes
        WebSocketFrameImpl(String text) {
            this.textData = text;
            this.binaryData = null;
        }

        // Konstruktor für Binärframes
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
    /// **WebSocket-Client-Ereignisse**
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
                JsonObject jsonMessage = gson.fromJson(message, JsonObject.class);
                if (jsonMessage.has("id")) {
                    int id = jsonMessage.get("id").getAsInt();
                    CompletableFuture<String> future = pendingCommands.remove(id);
                    if (future != null) {
                        future.complete(message);
                    }
                } else {
                    dispatcher.processEvent(message);
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                isClosed = true;
                System.out.println("WebSocket closed. Code: " + code + ", Reason: " + reason);
            }

            @Override
            public void onError(Exception ex) {
                System.err.println("WebSocket error occurred: " + ex.getMessage());
            }
        };
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // ToDo: Kann in Configure Methode integriert werden
    public void connect() throws InterruptedException {
        webSocketClient.connectBlocking();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public String sendAndWaitForResponse(Command command) {
        send(command);
        try {
            return receive(command.getId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for response", e);
        }
    }

    public CompletableFuture<String> send(Command command) {
        command.setId(getNextCommandId());
        JsonObject commandJson = command.toJson();
        CompletableFuture<String> future = new CompletableFuture<>();
        pendingCommands.put(command.getId(), future);
        webSocketClient.send(commandJson.toString());

        // Event für gesendeten Frame auslösen
        if (onFrameSent != null) {
            onFrameSent.accept(new WebSocketFrameImpl(commandJson.toString()));
        }

        return future;
    }

    /**
     * Blockiert, bis eine Antwort für das gegebene `commandId` empfangen wurde.
     */
    public String receive(int commandId) throws InterruptedException {
        CompletableFuture<String> future = pendingCommands.get(commandId);
        if (future == null) {
            throw new IllegalStateException("No pending command found with ID: " + commandId);
        }

        try {
            return future.get(20, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new RuntimeException("Timeout while waiting for response with ID: " + commandId, e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Error while waiting for response", e);
        } finally {
            pendingCommands.remove(commandId);
        }
    }

    private synchronized int getNextCommandId() {
        return ++commandCounter;
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
}
