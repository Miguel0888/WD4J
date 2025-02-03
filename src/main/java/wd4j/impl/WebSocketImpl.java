package wd4j.impl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import wd4j.api.*;
import wd4j.core.Dispatcher;
import wd4j.core.generic.Command;
import wd4j.impl.module.SessionService;
import wd4j.impl.support.DispatcherImpl;

import java.net.URI;
import java.util.concurrent.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class WebSocketImpl implements WebSocket {
    private final Dispatcher dispatcher; // Dispatcher-Objekt, das die Events verarbeitet
    private WebSocketClient webSocketClient;

    private int commandCounter = 0;
    private final ConcurrentHashMap<Integer, CompletableFuture<WebSocketFrame>> pendingCommands = new ConcurrentHashMap<>();
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
        callback.run();
        try {
            int commandId = getNextCommandId();
            CompletableFuture<WebSocketFrame> future = new CompletableFuture<>();
            pendingCommands.put(commandId, future);

            return future.get(20, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new RuntimeException("Timeout waiting for frame received", e);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error while waiting for frame received", e);
        }
    }

    @Override
    public WebSocketFrame waitForFrameSent(WaitForFrameSentOptions options, Runnable callback) {
        callback.run();
        try {
            int commandId = getNextCommandId();
            CompletableFuture<WebSocketFrame> future = new CompletableFuture<>();
            pendingCommands.put(commandId, future);

            return future.get(20, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new RuntimeException("Timeout waiting for frame sent", e);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error while waiting for frame sent", e);
        }
    }

    public CompletableFuture<WebSocketFrame> send(Command command) {
        command.setId(getNextCommandId());
        JsonObject commandJson = command.toJson();
        WebSocketFrameImpl frame = new WebSocketFrameImpl(commandJson.toString());

        CompletableFuture<WebSocketFrame> future = new CompletableFuture<>();
        pendingCommands.put(command.getId(), future);
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
                    } else {
                        dispatcher.process(message);
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

    public <T> void addEventListener(String eventName, Consumer<T> handler, Class<T> eventClass, SessionService sessionService) {
        dispatcher.addEventListener(eventName, handler, eventClass, sessionService);
    }

    public <T> void removeEventListener(String eventType, Consumer<T> listener, SessionService sessionService) {
        dispatcher.removeEventListener(eventType, listener, sessionService);
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
