package wd4j.impl.websocket;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import wd4j.api.WebSocketFrame;
import wd4j.impl.playwright.WebSocketImpl;
import wd4j.impl.support.EventDispatcher;
import wd4j.impl.webdriver.mapping.GsonMapperFactory;

import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.concurrent.atomic.AtomicReference;

public class WebSocketManager {
    private final WebSocketImpl webSocket;
    private final Gson gson = GsonMapperFactory.getGson(); // ✅ Nutzt zentrale Fabrik
    private int commandCounter = 0; // Zählt Befehle für eindeutige IDs

    private final EventDispatcher eventDispatcher;

    public WebSocketManager(WebSocketImpl webSocket) {
        this.webSocket = webSocket;
        this.eventDispatcher = new EventDispatcher();
        registerEventListener(eventDispatcher); // 🔥 Events aktivieren!
    }

    public EventDispatcher getEventDispatcher() {
        return eventDispatcher;
    }

    /**
     * Sendet einen Befehl über den WebSocket.
     *
     * @param WDCommand Das Command-Objekt, das gesendet werden soll.
     */
    public void send(WDCommand WDCommand) {
        WDCommand.setId(getNextCommandId()); // Command ID setzen
        String jsonCommand = gson.toJson(WDCommand);
        webSocket.send(jsonCommand); // Nachricht senden
    }

    /**
     * Sendet einen Befehl und wartet auf die Antwort. Die Antwort wird direkt als DTO des Typs `T` zurückgegeben.
     *
     * @param WDCommand   Der Befehl, der gesendet wird.
     * @param responseType Die Klasse des erwarteten DTOs.
     * @param <T> Der Typ der Antwort. Falls String.class gewählt wird, wird die Antwort als JSON-String zurückgegeben.
     * @return Ein deserialisiertes DTO der Klasse `T`.
     */
    public <T> T sendAndWaitForResponse(WDCommand WDCommand, Type responseType) {
        // Antwort vorbereiten (Listener registrieren)
        Predicate<WebSocketFrame> predicate = frame -> {
            try {
                JsonObject json = gson.fromJson(frame.text(), JsonObject.class);
                return json.has("id") && json.get("id").getAsInt() == WDCommand.getId();
            } catch (JsonSyntaxException e) {
                return false;
            }
        };
        CompletableFuture<String> receive = receive(predicate, String.class, true);

        // Befehl senden und auf Antwort warten
        send(WDCommand); // ✅ 1️⃣ Befehl senden
        try {
            String jsonString = receive.get(30, TimeUnit.SECONDS);
            // ✅ Direkt auf JSON-Objekt parsen
            JsonObject jsonObject = gson.fromJson(jsonString, JsonObject.class);

            // ✅ Falls `String.class`, gib einfach den JSON-String zurück
            if (responseType == String.class) {
                return (T) jsonString;
            }

            // ✅ "result" aus dem JSON extrahieren
            JsonElement resultElement = jsonObject.get("result");
            if (resultElement == null) {
                throw new RuntimeException("Response does not contain a 'result' field.");
            }

            // ✅ "result" direkt auf `responseType` mappen und zurückgeben!
            return gson.fromJson(resultElement, responseType);
        } catch (TimeoutException e) {
            throw new RuntimeException("Timeout while waiting for response.", e);
        }
        catch (InterruptedException | ExecutionException e)
        {
            if(e.getCause() instanceof WDException)
            {
                throw (WDException) e.getCause();
            }
            throw new RuntimeException("Error while waiting for response.", e);
        }
    }

    /**
     * Wartet asynchron auf eine empfangene Nachricht, die durch das Predicate gefiltert wird.
     *
     * @param predicate    Die Bedingung für die zu erwartende Nachricht.
     * @param responseType Die Klasse des erwarteten DTOs.
     * @param <T>          Der Typ der Antwort.
     * @throws WDException Falls eine Fehlerantwort empfangen wird.
     * @return Ein CompletableFuture mit der Antwort oder einem Fehler.
     */
    public <T> CompletableFuture<T> receive(Predicate<WebSocketFrame> predicate, Class<T> responseType)
    {
        return receive(predicate, responseType, true);
    }

    /**
     * Wartet asynchron auf eine empfangene Nachricht, die durch das Predicate gefiltert wird.
     *
     * @param predicate    Die Bedingung für die zu erwartende Nachricht.
     * @param responseType Die Klasse des erwarteten DTOs.
     * @param throwError   Falls `false`, wird keine Exception geworfen, sondern ein Fehler-DTO zurückgegeben.
     * @param <T>          Der Typ der Antwort.
     * @throws WDException Falls `throwError == true` und eine Fehlerantwort empfangen wird.
     * @return Ein CompletableFuture mit der Antwort oder einem Fehler.
     */
    public <T> CompletableFuture<T> receive(Predicate<WebSocketFrame> predicate, Class<T> responseType, boolean throwError) {
        CompletableFuture<T> future = new CompletableFuture<>();
        AtomicReference<Consumer<WebSocketFrame>> listenerRef = new AtomicReference<>();

        Consumer<WebSocketFrame> listener = frame -> {
//            System.out.println("[DEBUG] WebSocketManager received frame: " + frame.text());
            try {
                JsonObject json = gson.fromJson(frame.text(), JsonObject.class);

                // 🛠 Falls der Frame ein Fehler ist, direkt in `ErrorResponse` mappen
                if (json.has("type") && "error".equals(json.get("type").getAsString())) {
                    WDErrorResponse WDErrorResponse = gson.fromJson(frame.text(), wd4j.impl.websocket.WDErrorResponse.class);

                    if (throwError) {
                        future.completeExceptionally(new WDException(WDErrorResponse)); // ✅ Werfe Exception
                    } else {
                        future.complete(responseType.cast(WDErrorResponse)); // ✅ Gib `ErrorResponse` als DTO zurück
                    }
                    webSocket.offFrameReceived(listenerRef.get()); // Listener entfernen
                    return;
                }

                // Falls Predicate erfüllt → Antwort parsen
                if (predicate.test(frame)) {
                    // ✅ Falls `responseType == String.class`, einfach JSON-String direkt zurückgeben
                    T response;
                    if (responseType == String.class) {
                        response = responseType.cast(frame.text());
                    } else {
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

        listenerRef.set(listener); // 🛠 Hier wird die Variable gesetzt!
        webSocket.onFrameReceived(listenerRef.get()); // ✅ Sicherstellen, dass `listener` registriert ist

        return future;
    }

    /**
     * Registriert einen Event-Listener, der auf eingehende Events reagiert.
     *
     * @param eventDispatcher Der EventDispatcher, der die Events verarbeitet.
     */
    public void registerEventListener(EventDispatcher eventDispatcher) {
        webSocket.onFrameReceived(frame -> {
            try {
                JsonObject json = gson.fromJson(frame.text(), JsonObject.class);

                // Prüfen, ob es sich um ein Event handelt (kein "id"-Feld)
                if (json.has("method")) {
                    System.out.println("[DEBUG] WebSocketManager detected event: " + json.get("method").getAsString());
                    eventDispatcher.processEvent(json); // 🔥 Event an Dispatcher weitergeben
                }
            } catch (JsonSyntaxException e) {
                System.err.println("[ERROR] Failed to parse WebSocket event: " + e.getMessage());
            }
        });
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
