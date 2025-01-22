package wd4j.impl;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class WebSocketConnection {
    private final WebSocketClient webSocketClient;
    private final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();

    public WebSocketConnection(URI uri) {
        this.webSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                System.out.println("WebSocket connected");
            }

            @Override
            public void onMessage(String message) {
                try {
                    // Nachrichten in die Warteschlange legen
                    messageQueue.put(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("Fehler beim Verarbeiten der Nachricht: " + e.getMessage());
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                System.out.println("WebSocket closed with code " + code + ", reason: " + reason);
            }

            @Override
            public void onError(Exception ex) {
                System.err.println("WebSocket error: " + ex.getMessage());
            }
        };
    }

    public void connect() throws InterruptedException {
        webSocketClient.connectBlocking(); // Blockiert, bis die Verbindung hergestellt ist
    }

    public void send(String message) {
        System.out.println("Sende Nachricht: " + message);
        webSocketClient.send(message);
    }

    public String receive() throws InterruptedException {
        // Wartet auf die nächste Nachricht
        return messageQueue.take();
    }

    public void close() {
        webSocketClient.close();
    }
}
