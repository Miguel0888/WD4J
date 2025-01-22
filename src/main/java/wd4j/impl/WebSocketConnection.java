package wd4j.impl;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.net.URI;
import java.util.function.BiConsumer;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

public class WebSocketConnection {
    private final WebSocketClient webSocketClient;
    private final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
    private BiConsumer<Integer, String> onClose;

    public WebSocketConnection(URI uri) {
        this.webSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                System.out.println("WebSocket connected: " + handshakedata.getHttpStatusMessage());
            }

            @Override
            public void onMessage(String message) {
                try {
                    System.out.println("Received message: " + message);
                    messageQueue.put(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("Error processing message: " + e.getMessage());
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                System.out.println("WebSocketConnection closed. Code: " + code + ", Reason: " + reason);
                if (onClose != null) {
                    onClose.accept(code, reason);
                }
            }

            @Override
            public void onError(Exception ex) {
                System.err.println("WebSocket error occurred:");
                ex.printStackTrace();
            }
        };
    }

    public void connect() throws InterruptedException {
        webSocketClient.connectBlocking();
    }

    public void send(String message) {
        System.out.println("Sending message: " + message);
        webSocketClient.send(message);
    }

    public String receive() throws InterruptedException {
        // Set a timeout for receiving messages
        String message = messageQueue.poll(10, TimeUnit.SECONDS);
        if (message == null) {
            throw new RuntimeException("Timeout while waiting for a WebSocket response");
        }
        return message;
    }

    public void close() {
        webSocketClient.close();
    }

    /**
     * Set a consumer to be called when the WebSocket connection is closed (Callback).
     *
     * @param onClose Consumer to be called when the WebSocket connection is closed
     */
    public void setOnClose(BiConsumer<Integer, String> onClose) {
        this.onClose = onClose;
    }
}
