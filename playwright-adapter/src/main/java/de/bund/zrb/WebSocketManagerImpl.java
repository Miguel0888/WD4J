package de.bund.zrb;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.microsoft.playwright.WebSocketFrame;
import de.bund.zrb.support.mapping.GsonMapperFactory;
import de.bund.zrb.api.WDCommand;
import de.bund.zrb.websocket.WDErrorResponse;
import de.bund.zrb.websocket.WDException;
import de.bund.zrb.api.WebSocketManager;

import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class WebSocketManagerImpl implements WebSocketManager {
    private final Gson gson = GsonMapperFactory.getGson(); // ✅ Nutzt zentrale Fabrik

    private final WebSocketImpl webSocket; // ToDo: Should be WebSocket instead of WebSocketImpl

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Deprecated // since WebSocketConnection should not be a singleton anymore?
    private static volatile WebSocketManagerImpl instance; // ToDo: Remove singleton pattern

    @Deprecated // since WebSocketConnection should not be a singleton anymore?
    public WebSocketManagerImpl(WebSocketImpl webSocket) {
        this.webSocket = webSocket;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////



    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Sendet einen Befehl über den WebSocket.
     *
     * @param command Das Command-Objekt, das gesendet werden soll.
     */
    public void send(WDCommand command) {
        if(webSocket.isClosed())
        { // ToDo: Find a better solution, the problem is a new connection removes all listeners
            throw new RuntimeException("WebSocket connection is closed. Please reestablish the connection.");
//            this.webSocket = WebSocketImpl.getInstance();
//            registerEventListener(eventDispatcher);
        }
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
    public <T> T sendAndWaitForResponse(WDCommand command, Type responseType) {
        // Antwort vorbereiten (Listener registrieren)
        Predicate<WebSocketFrame> predicate = frame -> {
            try {
                JsonObject json = gson.fromJson(frame.text(), JsonObject.class);
                return json.has("id") && json.get("id").getAsInt() == command.getId();
            } catch (JsonSyntaxException e) {
                return false;
            }
        };
        CompletableFuture<String> receive = receive(predicate, String.class, true);

        // Befehl senden und auf Antwort warten
        send(command); // ✅ 1️⃣ Befehl senden
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
    @Deprecated
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
                    WDErrorResponse WDErrorResponse = gson.fromJson(frame.text(), WDErrorResponse.class);

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
    @Override
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

    public boolean isConnected() {
        return webSocket.isConnected();
        // ToDo: Check the session, too? (e.g. if the session is still alive, otherwise the user is not able to send commands)
        //  You may use a WebDriver BiDi command to check the session status?
        //  -> newSession() Command has to be send otherwise
    }
}
