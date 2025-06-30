package de.bund.zrb.controller;

import de.bund.zrb.service.RecorderService;
import org.java_websocket.server.WebSocketServer;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.WebSocket;
import java.net.InetSocketAddress;
import java.util.function.Consumer;

@Deprecated
public class CallbackWebSocketServer extends WebSocketServer {

    private static CallbackWebSocketServer callbackWebSocketServer;
    private static boolean running = false;  // ✅ Zustand speichern

    private final Consumer<String> eventConsumer;

    public CallbackWebSocketServer(int port, Consumer<String> eventConsumer) {
        super(new InetSocketAddress(port));
        this.eventConsumer = eventConsumer;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("✅ Neue Verbindung: " + conn.getRemoteSocketAddress());
        conn.send("🔗 Verbindung erfolgreich!");
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

    public static synchronized void toggleCallbackServer(boolean activate) {
        if (activate && !running) {
            callbackWebSocketServer = new CallbackWebSocketServer(8080, message -> {
                RecorderService.getInstance().recordAction(message);
            });
            try {
                callbackWebSocketServer.start();
                running = true;
                System.out.println("✅ Recorder gestartet.");
            } catch (Exception e) {
                e.printStackTrace();
                running = false; // Safety
            }
        } else if (!activate && running) {
            try {
                callbackWebSocketServer.stop();
                running = false;
                System.out.println("⏸️ Recorder gestoppt.");
            } catch (Exception e) {
                e.printStackTrace();
                running = true; // Falls Stop fehlschlägt, Flag bleibt an
            }
        } else {
            System.out.println("ℹ️ Keine Änderung: Recorder bleibt im aktuellen Zustand.");
        }
    }

    public static boolean isRunning() {
        return running;
    }
}
