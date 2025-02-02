package wd4j.impl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import wd4j.api.WebSocket;
import wd4j.api.WebSocketFrame;
import wd4j.impl.generic.Command;

import java.net.URI;
import java.util.concurrent.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class WebSocketImpl implements WebSocket {

    private WebSocketClient webSocketClient;
    private final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
    private final ConcurrentHashMap<Integer, CompletableFuture<String>> pendingCommands = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();

    private int commandCounter = 0;
    private BiConsumer<Integer, String> onClose;
    private Consumer<WebSocketFrame> onFrameReceived;
    private Consumer<WebSocketFrame> onFrameSent;
    private Consumer<String> onSocketError;

    private boolean isClosed = false;
    private String url;

    public WebSocketImpl() {}

    public WebSocketImpl(URI uri) {
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
        callback.run();
        try {
            return new WebSocketFrameImpl(messageQueue.take());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    @Override
    public WebSocketFrame waitForFrameSent(WaitForFrameSentOptions options, Runnable callback) {
        callback.run(); // Führt die Aktion aus, die den Frame senden soll.

        try {
            // Blockiert, bis eine Nachricht gesendet wurde.
            String sentMessage = messageQueue.take();
            return new WebSocketFrameImpl(sentMessage);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
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
                        CompletableFuture<String> future = pendingCommands.remove(id);
                        if (future != null) {
                            future.complete(message);
                        }
                    } else if (jsonMessage.has("type") && "event".equals(jsonMessage.get("type").getAsString())) {
                        messageQueue.put(message);
                    } else {
                        messageQueue.put(message);
                    }

                    // Verarbeite WebSocket-Frames
                    if (onFrameReceived != null) {
                        onFrameReceived.accept(new WebSocketFrameImpl(message));
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    if (onSocketError != null) {
                        onSocketError.accept("Error processing message: " + e.getMessage());
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

    public void connect() throws InterruptedException {
        webSocketClient.connectBlocking();
    }

    public String sendAndWaitForResponse(Command command) {
        // Kommando in eine asynchrone Anfrage umwandeln
        CompletableFuture<String> future = send(command);

        try {
            // Blockierend auf die Antwort warten
            return future.get(20, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new RuntimeException("Timeout while waiting for response with ID: " + command.getId(), e);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error while waiting for response", e);
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
