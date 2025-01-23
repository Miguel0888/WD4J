// package wd4j.impl;

// import org.java_websocket.client.WebSocketClient;
// import org.java_websocket.handshake.ServerHandshake;

// import java.net.URI;
// import java.util.concurrent.BlockingQueue;
// import java.util.concurrent.LinkedBlockingQueue;

// public class BiDiWebSocketClient extends WebSocketClient {

//     // Warteschlange für empfangene Nachrichten
//     private final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();

//     public BiDiWebSocketClient(URI serverUri) {
//         super(serverUri);
//     }

//     @Override
//     public void onOpen(ServerHandshake handshakedata) {
//         System.out.println("WebSocket connected");
//     }

//     @Override
//     public void onMessage(String message) {
//         System.out.println("Received: " + message);
//         try {
//             // Nachricht in die Warteschlange legen
//             messageQueue.put(message);
//         } catch (InterruptedException e) {
//             Thread.currentThread().interrupt();
//             System.err.println("Fehler beim Verarbeiten der Nachricht: " + e.getMessage());
//         }
//     }

//     @Override
//     public void onClose(int code, String reason, boolean remote) {
//         System.out.println("WebSocket closed with exit code " + code + ", reason: " + reason);
//     }

//     @Override
//     public void onError(Exception ex) {
//         System.err.println("WebSocket error: " + ex.getMessage());
//     }

//     // Nachricht abrufen
//     public String receive() throws InterruptedException {
//         return messageQueue.take();
//     }
// }
