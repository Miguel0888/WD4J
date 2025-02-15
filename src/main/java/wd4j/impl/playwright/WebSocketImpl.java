package wd4j.impl.playwright;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import wd4j.api.*;
import wd4j.impl.websocket.Command;

import java.net.URI;
import java.util.concurrent.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class WebSocketImpl implements WebSocket {
    private WebSocketClient webSocketClient;
    private final Gson gson = new Gson();
    private boolean isClosed = false;
    private String url;

    private final ConcurrentHashMap<Integer, CompletableFuture<WebSocketFrame>> pendingCommands = new ConcurrentHashMap<>();
    private final BlockingQueue<WebSocketFrame> sentFrames = new LinkedBlockingQueue<>();

    private BiConsumer<Integer, String> onClose;
    private Consumer<WebSocketFrame> onFrameReceived;
    private Consumer<WebSocketFrame> onFrameSent;
    private Consumer<String> onSocketError;
    private int commandCounter;

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

    /**
     * Sends a command and returns a CompletableFuture that completes when the response arrives.
     */
    public CompletableFuture<WebSocketFrame> send(Command command) {
        int commandId = getNextCommandId();
        command.setId(commandId);

        CompletableFuture<WebSocketFrame> future = new CompletableFuture<>();
        pendingCommands.put(commandId, future);

        String jsonCommand = gson.toJson(command);
        webSocketClient.send(jsonCommand);

        WebSocketFrameImpl frame = new WebSocketFrameImpl(jsonCommand);
        sentFrames.offer(frame); // Frame wird in die Queue für `waitForFrameSent` gelegt.

        if (onFrameSent != null) {
            onFrameSent.accept(frame);
        }

        return future;
    }

    /**
     * Waits for a frame that matches the predicate.
     */
    @Override
    public WebSocketFrame waitForFrameReceived(WaitForFrameReceivedOptions options, Runnable callback) {
        callback.run(); // Führt das übergebene Kommando aus (z.B. Senden eines Commands).

        Predicate<WebSocketFrame> predicate = options.predicate != null ? options.predicate : frame -> true;
        CompletableFuture<WebSocketFrame> future = new CompletableFuture<>();

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                while (!isClosed) {
                    for (CompletableFuture<WebSocketFrame> pending : pendingCommands.values()) {
                        WebSocketFrame frame = pending.get(100, TimeUnit.MILLISECONDS);
                        if (predicate.test(frame)) {
                            future.complete(frame);
                            return;
                        }
                    }
                    Thread.sleep(10);
                }
            } catch (InterruptedException | ExecutionException | TimeoutException ignored) {}
        });

        try {
            return future.get((long) (options.timeout != null ? options.timeout : 30_000), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Timeout waiting for frame received", e);
        }
    }

    /**
     * Waits for the last sent frame to be processed.
     */
    @Override
    public WebSocketFrame waitForFrameSent(WaitForFrameSentOptions options, Runnable callback) {
        callback.run(); // Führt das übergebene Kommando aus.

        try {
            long timeout = options.timeout != null ? options.timeout.longValue() : 30_000;
            return sentFrames.poll(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException("Timeout waiting for frame sent", e);
        }
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
                        if (onFrameReceived != null) {
                            onFrameReceived.accept(new WebSocketFrameImpl(message));
                        }
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
    /// **Overridden WebSocket-Methods**
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
