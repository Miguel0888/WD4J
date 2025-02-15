package wd4j.impl.websocket;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import wd4j.api.WebSocket;
import wd4j.api.WebSocketFrame;
import wd4j.impl.playwright.WebSocketImpl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.concurrent.atomic.AtomicReference;

public class CommunicationManager {
    private final WebSocketImpl webSocket;
    private final Gson gson = new Gson();
    private int commandCounter = 0; // Zählt Befehle für eindeutige IDs

    public CommunicationManager(WebSocketImpl webSocket) {
        this.webSocket = webSocket;
    }

    /**
     * Sendet einen Befehl über den WebSocket.
     *
     * @param command Das Command-Objekt, das gesendet werden soll.
     */
    public void send(Command command) {
        command.setId(getNextCommandId()); // Command ID setzen
        String jsonCommand = gson.toJson(command);
        webSocket.send(jsonCommand); // Nachricht senden
    }

    /**
     * Sendet einen Befehl und wartet auf die Antwort. Die Antwort wird direkt als DTO des Typs `T` zurückgegeben.
     *
     * @param command   Der Befehl, der gesendet wird.
     * @param responseType Die Klasse des erwarteten DTOs.
     * @param <T> Der Typ der Antwort.
     * @return Ein deserialisiertes DTO der Klasse `T`.
     */
    public <T> T sendAndWaitForResponse(Command command, Class<T> responseType) {
        send(command); // 1️⃣ Senden des Commands

        // 2️⃣ Predicate: Prüft, ob das empfangene Frame die richtige ID hat
        Predicate<WebSocketFrame> predicate = frame -> {
            try {
                T response = gson.fromJson(frame.text(), responseType);
                return response != null;
            } catch (JsonSyntaxException e) {
                return false;
            }
        };

        // 3️⃣ Warte auf die Antwort mit `receive()`
        try {
            return receive(predicate, responseType).get(30, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new RuntimeException("Timeout while waiting for response.", e);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error while waiting for response.", e);
        }
    }

    /**
     * Wartet asynchron auf eine empfangene Nachricht, die durch das Predicate gefiltert wird.
     *
     * @param predicate Die Bedingung für die zu erwartende Nachricht.
     * @param responseType Die Klasse des erwarteten DTOs.
     * @param <T> Der Typ der Antwort.
     * @return Ein CompletableFuture mit der Antwort.
     */
    public <T> CompletableFuture<T> receive(Predicate<WebSocketFrame> predicate, Class<T> responseType) {
        CompletableFuture<T> future = new CompletableFuture<>();

        // 🛠 `AtomicReference` für self-referenzierende Lambda
        AtomicReference<Consumer<WebSocketFrame>> listenerRef = new AtomicReference<>();

        Consumer<WebSocketFrame> listener = frame -> {
            try {
                if (predicate.test(frame)) {
                    T response = gson.fromJson(frame.text(), responseType);
                    if (response != null) {
                        future.complete(response);
                        webSocket.offFrameReceived(listenerRef.get()); // ✅ Listener wird korrekt entfernt
                    }
                }
            } catch (JsonSyntaxException ignored) {}
        };

        listenerRef.set(listener); // 🛠 Hier wird die Variable gesetzt!
        webSocket.onFrameReceived(listenerRef.get()); // ✅ Sicherstellen, dass `listener` registriert ist

        return future;
    }


    /**
     * Gibt eine neue eindeutige Command-ID zurück.
     *
     * @return Eine inkrementelle ID.
     */
    private synchronized int getNextCommandId() {
        return ++commandCounter;
    }

    public boolean isConnected() {
        return webSocket.isConnected();
    }
}
