package wd4j.impl.websocket;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
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
     * @param <T> Der Typ der Antwort. Falls String.class gewählt wird, wird die Antwort als JSON-String zurückgegeben.
     * @return Ein deserialisiertes DTO der Klasse `T`.
     */
    public <T> T sendAndWaitForResponse(Command command, Class<T> responseType) {
        send(command); // 1️⃣ Senden des Commands

        // 2️⃣ Predicate: Prüft, ob das empfangene Frame die richtige ID hat
        Predicate<WebSocketFrame> predicate = frame -> {
            try {
                if (responseType == String.class) {
                    // Falls `String.class`, müssen wir direkt das JSON analysieren
                    JsonObject json = gson.fromJson(frame.text(), JsonObject.class);
                    return json.has("id") && json.get("id").getAsInt() == command.getId();
                } else {
                    // Normales DTO-Mapping mit Gson
                    T response = gson.fromJson(frame.text(), responseType);
                    return response != null;
                }
            } catch (JsonSyntaxException e) {
                System.out.println("[ERROR] JSON Parsing-Fehler: " + e.getMessage());
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
        AtomicReference<Consumer<WebSocketFrame>> listenerRef = new AtomicReference<>();

        Consumer<WebSocketFrame> listener = frame -> {
//            System.out.println("[DEBUG] Frame empfangen: " + frame.text());

            try {
                JsonObject json = gson.fromJson(frame.text(), JsonObject.class);

                // 🛠 Falls der Frame ein Fehler ist, direkt in `ErrorResponse` mappen
                if (json.has("type") && "error".equals(json.get("type").getAsString())) {
                    ErrorResponse errorResponse = gson.fromJson(frame.text(), ErrorResponse.class);
                    future.completeExceptionally(new WebSocketErrorException(errorResponse)); // ✅ Fehler als `ErrorResponse` zurückgeben
                    webSocket.offFrameReceived(listenerRef.get()); // Listener entfernen
                    return;
                }

                // Falls Predicate erfüllt → Antwort parsen
                if (predicate.test(frame)) {
                    // ✅ Falls `responseType == String.class`, einfach JSON-String direkt zurückgeben
                    T response;
                    if (responseType == String.class) {
                        response = responseType.cast(frame.text());
                    }
                    else
                    {
                        // ✅ Normales Mapping für DTOs
                        response = gson.fromJson(frame.text(), responseType);
                    }
                    if (response != null) {
                        future.complete(response);
                        webSocket.offFrameReceived(listenerRef.get()); // Listener entfernen
                    }
                } else {
                    System.out.println("[DEBUG] Frame erfüllt Predicate NICHT! Ignoriert.");
                }
            } catch (JsonSyntaxException e) {
                System.out.println("[ERROR] JSON Parsing-Fehler: " + e.getMessage());
            }
        };

        listenerRef.set(listener);
        webSocket.onFrameReceived(listenerRef.get());
//        System.out.println("[DEBUG] Listener erfolgreich registriert.");

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
