package wd4j.impl.playwright;

import com.google.gson.Gson;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import wd4j.api.*;
import wd4j.impl.websocket.Command;

import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class WebSocketImpl implements WebSocket {
    private static final int MAX_QUEUE_SIZE = 100;
    private WebSocketClient webSocketClient;
    private final Gson gson = new Gson();
    private boolean isClosed = false;
    private String url;

    private final BlockingQueue<WebSocketFrame> sentFrames = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
    private final BlockingQueue<WebSocketFrame> receivedFrames = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);

    private final List<Consumer<WebSocket>> onCloseListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<WebSocketFrame>> onFrameReceivedListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<WebSocketFrame>> onFrameSentListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<String>> onSocketErrorListeners = new CopyOnWriteArrayList<>();

    public WebSocketImpl(URI uri) {
        createAndConfigureWebSocketClient(uri);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// **WebSocket-Event-Listener**
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onClose(Consumer<WebSocket> handler) {
        onCloseListeners.add(handler);
    }

    @Override
    public void offClose(Consumer<WebSocket> handler) {
        onCloseListeners.remove(handler);
    }

    @Override
    public void onFrameReceived(Consumer<WebSocketFrame> handler) {
        onFrameReceivedListeners.add(handler);
    }

    @Override
    public void offFrameReceived(Consumer<WebSocketFrame> handler) {
        onFrameReceivedListeners.remove(handler);
    }

    @Override
    public void onFrameSent(Consumer<WebSocketFrame> handler) {
        onFrameSentListeners.add(handler);
    }

    @Override
    public void offFrameSent(Consumer<WebSocketFrame> handler) {
        onFrameSentListeners.remove(handler);
    }

    @Override
    public void onSocketError(Consumer<String> handler) {
        onSocketErrorListeners.add(handler);
    }

    @Override
    public void offSocketError(Consumer<String> handler) {
        onSocketErrorListeners.remove(handler);
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

    @Override
    public WebSocketFrame waitForFrameReceived(WaitForFrameReceivedOptions options, Runnable callback) {
        callback.run(); // Führt das übergebene Kommando aus.

        Predicate<WebSocketFrame> predicate = options.predicate != null ? options.predicate : frame -> true;
        long timeout = options.timeout != null ? options.timeout.longValue() : 30_000;

        long startTime = System.currentTimeMillis();
        while (true) {
            if (isClosed) {
                throw new RuntimeException("WebSocket was closed before a frame was received.");
            }

            long remainingTime = timeout - (System.currentTimeMillis() - startTime);
            if (remainingTime <= 0) {
                throw new RuntimeException("Timeout while waiting for frame received.");
            }

            try {
                WebSocketFrame frame = receivedFrames.poll(remainingTime, TimeUnit.MILLISECONDS);
                if (frame == null) throw new RuntimeException("Timeout while waiting for frame received.");
                if (predicate.test(frame)) return frame;
            } catch (InterruptedException ignored) {}
        }
    }

    @Override
    public WebSocketFrame waitForFrameSent(WaitForFrameSentOptions options, Runnable callback) {
        callback.run();

        Predicate<WebSocketFrame> predicate = options.predicate != null ? options.predicate : frame -> true;
        long timeout = options.timeout != null ? options.timeout.longValue() : 30_000;

        long startTime = System.currentTimeMillis();
        while (true) {
            if (isClosed) {
                throw new RuntimeException("WebSocket was closed before the frame was sent.");
            }

            long remainingTime = timeout - (System.currentTimeMillis() - startTime);
            if (remainingTime <= 0) {
                throw new RuntimeException("Timeout while waiting for frame sent.");
            }

            try {
                WebSocketFrame frame = sentFrames.poll(remainingTime, TimeUnit.MILLISECONDS);
                if (frame == null) throw new RuntimeException("Timeout while waiting for frame sent.");
                if (predicate.test(frame)) return frame;
            } catch (InterruptedException ignored) {}
        }
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
                    WebSocketFrameImpl frame = new WebSocketFrameImpl(message);

                    if (!receivedFrames.offer(frame)) {
                        receivedFrames.poll();
                        receivedFrames.offer(frame);
                    }

                    onFrameReceivedListeners.forEach(listener -> listener.accept(frame));
                } catch (Exception e) {
                    onSocketErrorListeners.forEach(listener -> listener.accept("Error processing WebSocket message: " + e.getMessage()));
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                isClosed = true;
                System.out.println("WebSocket closed. Code: " + code + ", Reason: " + reason);

                // Alle registrierten `onClose`-Listener mit `this` benachrichtigen
                onCloseListeners.forEach(listener -> listener.accept(WebSocketImpl.this));
            }

            @Override
            public void onError(Exception ex) {
                onSocketErrorListeners.forEach(listener -> listener.accept("WebSocket error occurred: " + ex.getMessage()));
            }
        };
    }

    public void connect() throws InterruptedException {
        webSocketClient.connectBlocking();
    }

    public void close() {
        isClosed = true;
        receivedFrames.clear();
        sentFrames.clear();
        webSocketClient.close();
        System.out.println("WebSocket connection closed.");
    }

    public boolean isConnected() {
        return webSocketClient.isOpen();
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
