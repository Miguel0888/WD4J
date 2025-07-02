package de.bund.zrb;

import com.microsoft.playwright.WebSocket;
import com.microsoft.playwright.WebSocketFrame;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class WebSocketImpl implements WebSocket {
    private WebSocketClient webSocketClient;
    private boolean isClosed = false;
    private String url;

    private final List<Consumer<WebSocket>> onCloseListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<WebSocketFrame>> onFrameReceivedListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<WebSocketFrame>> onFrameSentListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<String>> onSocketErrorListeners = new CopyOnWriteArrayList<>();
    private double timeout = 30_000.0;

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
     * Nutzt ein CompletableFuture<WebSocketFrame> als eine Art "Versprechen", dass irgendwann in der Zukunft ein
     * WebSocketFrame ankommt. Das passiert in 3 Schritten:
     *
     * 1️⃣ Listener registrieren: Wenn ein neues Frame kommt, prüfen wir, ob es passt.
     * 2️⃣ Future abschließen: Falls es passt, wird das CompletableFuture abgeschlossen (future.complete(frame)).
     * 3️⃣ Antwort abholen: future.get(timeout) wartet, bis das Frame ankommt oder das Timeout überschritten wird.
     *
     * @param options
     * @param callback Callback that performs the action triggering the event.
     * @return
     */
    @Override
    public WebSocketFrame waitForFrameReceived(WaitForFrameReceivedOptions options, Runnable callback) {
        callback.run(); // Führt das übergebene Kommando aus.

        Double timeout = options != null && options.timeout != null ? options.timeout: this.timeout;
        Predicate<WebSocketFrame> predicate = options.predicate != null ? options.predicate : frame -> true;

        CompletableFuture<WebSocketFrame> future = new CompletableFuture<>();

        // 1️⃣ Listener wird registriert und prüft, ob das Frame passt
        Consumer<WebSocketFrame> listener = frame -> {
            if (predicate.test(frame)) {
                future.complete(frame); // 2️⃣ Schließt das Future ab → Antwort erhalten
            }
        };

        onFrameReceived(listener); // Listener hinzufügen

        try {
            // 3️⃣ Wartet auf das passende Frame oder Timeout
            if(timeout == null || timeout == 0)
            {
                return future.get(); // no timeout
            } else {
                return future.get(timeout.longValue(), TimeUnit.MILLISECONDS);
            }
        } catch (TimeoutException e) {
            throw new RuntimeException("Timeout while waiting for frame received.");
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error while waiting for frame received.", e);
        } finally {
            offFrameReceived(listener); // Listener wieder entfernen, um Speicherlecks zu verhindern
        }
    }

    /**
     * Nutzt ein CompletableFuture<WebSocketFrame> als eine Art "Versprechen", dass irgendwann in der Zukunft ein
     * WebSocketFrame ankommt. Das passiert in 3 Schritten:
     *
     * 1️⃣ Listener registrieren: Wenn ein neues Frame kommt, prüfen wir, ob es passt.
     * 2️⃣ Future abschließen: Falls es passt, wird das CompletableFuture abgeschlossen (future.complete(frame)).
     * 3️⃣ Antwort abholen: future.get(timeout) wartet, bis das Frame ankommt oder das Timeout überschritten wird.
     *
     * @param options
     * @param callback Callback that performs the action triggering the event.
     * @return
     */
    @Override
    public WebSocketFrame waitForFrameSent(WaitForFrameSentOptions options, Runnable callback) {
        callback.run(); // Führt das übergebene Kommando aus.

        Predicate<WebSocketFrame> predicate = options.predicate != null ? options.predicate : frame -> true;
        long timeout = options.timeout != null ? options.timeout.longValue() : 30_000;

        CompletableFuture<WebSocketFrame> future = new CompletableFuture<>();

        // 1️⃣ Listener registrieren
        Consumer<WebSocketFrame> listener = frame -> {
            if (predicate.test(frame)) {
                future.complete(frame); // 2️⃣ Antwort erhalten!
            }
        };

        onFrameSent(listener); // Listener hinzufügen

        try {
            // 3️⃣ Warten auf das passende Frame oder Timeout
            return future.get(timeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new RuntimeException("Timeout while waiting for frame sent.");
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error while waiting for frame sent.", e);
        } finally {
            offFrameSent(listener); // Listener wieder entfernen
        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// **WebSocket-Verwaltung**
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public WebSocketImpl(URI uri, Double timeout) {
        this(uri);
        this.timeout = timeout;
    }


    public WebSocketImpl(URI uri) {
        this.url = uri.toString();
        webSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                System.out.println("WebSocket connected: " + handshakedata.getHttpStatusMessage());
            }

            @Override
            public void onMessage(String message) {
                System.out.println("[WebSocket] Message received: " + message);
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
                onCloseListeners.forEach(listener -> listener.accept(WebSocketImpl.this));
            }

            @Override
            public void onError(Exception ex) {
                onSocketErrorListeners.forEach(listener -> listener.accept("WebSocket error occurred: " + ex.getMessage()));
            }

            // Hier fangen wir ALLE ausgehenden Nachrichten ab und benachrichtigen `onFrameSentListeners`
            @Override
            public void send(String message) {
                System.out.println("[WebSocket] Message sent: " + message);
                super.send(message); // Die Nachricht wirklich senden

                WebSocketFrameImpl frame = new WebSocketFrameImpl(message);

                onFrameSentListeners.forEach(listener -> listener.accept(frame));
            }
        };
    }

    public void connect() throws InterruptedException {
        webSocketClient.connectBlocking();
    }

    public void close() {
        isClosed = true;
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
