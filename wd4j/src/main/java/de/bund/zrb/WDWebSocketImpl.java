package de.bund.zrb;

import de.bund.zrb.api.WDWebSocket;
import de.bund.zrb.api.WebSocketFrame;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Predicate;


/**
 * ToDo: WebSocket & WebSockeFrames wurden hier fälschlicherweise eingesetzt. Sie sind für Verbindungen der Seite selbst gedacht,
 * nicht für DIE verbindung zum Browser über WebDriverBidi & WebSocket
 *
 */
public class WDWebSocketImpl implements WDWebSocket {
    private WebSocketClient webSocketClient;
    private boolean isClosed = false;
    private String url;

    private final List<Consumer<WDWebSocket>> onCloseListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<WebSocketFrame>> onFrameReceivedListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<WebSocketFrame>> onFrameSentListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<String>> onSocketErrorListeners = new CopyOnWriteArrayList<>();
    private double timeout = 30_000.0; // in ms

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// **WebSocket-Event-Listener**
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onClose(Consumer<WDWebSocket> handler) {
        onCloseListeners.add(handler);
    }

    @Override
    public void offClose(Consumer<WDWebSocket> handler) {
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

    public void send(String jsonCommand) {
        if (isClosed) {
            throw new RuntimeException("Cannot send message: WebSocket is closed.");
        }

        webSocketClient.send(jsonCommand); // Nachricht senden
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// **WebSocket-Frames Implementierung**
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class WebSocketFrameImpl implements WebSocketFrame {
        private final byte[] binaryData;
        private final String textData;

        WebSocketFrameImpl(String text) {
            this.textData = text;
            this.binaryData = null;
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
    /// **WebSocket-Verwaltung**
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public WDWebSocketImpl(URI uri, Double timeout) {
        this(uri);
        this.timeout = timeout;
        // Connection-Lost-Detection-Intervall auf 0 setzen → dann wird keine Ping/Pong-Prüfung gemacht:
        webSocketClient.setConnectionLostTimeout((int) Math.round(timeout / 1000.0));

        try {
            connect();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    public WDWebSocketImpl(URI uri) {
        this.url = uri.toString();
        webSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                if (Boolean.getBoolean("wd4j.log.browser")) {
                    System.out.println("[Browser] WebSocket connected: " + handshakedata.getHttpStatusMessage());
                }
            }

            @Override
            public void onMessage(String message) {
                if (Boolean.getBoolean("wd4j.log.websocket")) {
                    System.out.println("[WebSocket] Message received: " + message);
                }
                try {
                    WebSocketFrameImpl frame = new WebSocketFrameImpl(message);

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
                onCloseListeners.forEach(listener -> listener.accept(WDWebSocketImpl.this));
            }

            @Override
            public void onError(Exception ex) {
                onSocketErrorListeners.forEach(listener -> listener.accept("WebSocket error occurred: " + ex.getMessage()));
            }

            // Hier fangen wir ALLE ausgehenden Nachrichten ab und benachrichtigen `onFrameSentListeners`
            @Override
            public void send(String message) {
                if (Boolean.getBoolean("wd4j.log.websocket")) {
                    System.out.println("[WebSocket] Message sent: " + message);
                }
                super.send(message); // Die Nachricht wirklich senden

                WebSocketFrameImpl frame = new WebSocketFrameImpl(message);

                onFrameSentListeners.forEach(listener -> listener.accept(frame));
            }
        };
    }

    private void connect() throws InterruptedException {
        webSocketClient.connectBlocking();
    }

    public void close() {
        isClosed = true;
        webSocketClient.close();
        if (Boolean.getBoolean("wd4j.log.websocket")) {
            System.out.println("WebSocket connection closed.");
        }
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
