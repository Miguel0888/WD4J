package app.controller;

import org.java_websocket.server.WebSocketServer;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.WebSocket;
import java.net.InetSocketAddress;
import java.util.function.Consumer;

/**
 * Works as Callback for the Browser. In WebDriverBiDi you could use channels (and messages) alternatively,
 * but the implementation maybe more complex then.
 */
@Deprecated // since script.ChannelValue might be used for Callbacks (will lead to Message Events)
public class CallbackWebSocketServer extends WebSocketServer {
    Consumer<String> eventConsumer;

    public CallbackWebSocketServer(int port, Consumer<String> eventConsumer) {
        super(new InetSocketAddress(port));
        this.eventConsumer = eventConsumer;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("✅ Neue Verbindung: " + conn.getRemoteSocketAddress());
        conn.send("🔗 Verbindung erfolgreich!"); // Antwort an den Client
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("❌ Verbindung geschlossen: " + conn.getRemoteSocketAddress() + " Grund: " + reason);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("📌 Geklickter Selektor: " + message);
        eventConsumer.accept(message);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("⚠️ Fehler: " + ex.getMessage());
    }

    @Override
    public void onStart() {
        System.out.println("🚀 WebSocket-Server läuft auf ws://localhost:8080");
    }
}
