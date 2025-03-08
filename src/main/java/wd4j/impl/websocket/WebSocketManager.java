package wd4j.impl.websocket;

import app.Main;
import app.controller.CallbackWebSocketServer;
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
    private final Gson gson = GsonMapperFactory.getGson(); // ✅ Nutzt zentrale Fabrik

    private final EventDispatcher eventDispatcher;
    private WebSocketImpl webSocket; // ToDo: Remove this, since it is a workaround to reorganize if connection is closed

    private static volatile WebSocketManager instance;

    @Deprecated // since JSON Data might be received via Message Events (see WebDriverBiDi ChannelValue)
    private CallbackWebSocketServer callbackWebSocketServer;

    private WebSocketManager() {
        this.webSocket = WebSocketImpl.getInstance();
        this.eventDispatcher = new EventDispatcher();
        registerEventListener(eventDispatcher); // 🔥 Events aktivieren!

        toggleCallbackServer(true); // ✅ Callback-Server aktivieren
    }

    @Deprecated // since script.ChannelValue might be used for Callbacks (will lead to Message Events)
    private void toggleCallbackServer(boolean activate) {
        if (activate) {
            callbackWebSocketServer = new CallbackWebSocketServer(8080, message ->
                    Main.scriptLog.append(message + System.lineSeparator()));
            callbackWebSocketServer.start();
        } else {
            try {
                callbackWebSocketServer.stop();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static WebSocketManager getInstance() {
        if (instance == null) {
            synchronized (WebSocketManager.class) {
                if (instance == null) {
                    instance = new WebSocketManager();
                }
            }
        }
        return instance;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public EventDispatcher getEventDispatcher() {
        return eventDispatcher;
    }

    /**
     * Sendet einen Befehl über den WebSocket.
     *
     * @param command Das Command-Objekt, das gesendet werden soll.
     */
    public void send(WDCommand command) {
        if(webSocket.isClosed())
        { // ToDo: Find a better solution, the problem is a new connection removes all listeners
            this.webSocket = WebSocketImpl.getInstance();
            registerEventListener(eventDispatcher);
        }
        String jsonCommand = gson.toJson(command);
        WebSocketImpl.getInstance().send(jsonCommand); // Nachricht senden
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
                    WDErrorResponse WDErrorResponse = gson.fromJson(frame.text(), wd4j.impl.websocket.WDErrorResponse.class);

                    if (throwError) {
                        future.completeExceptionally(new WDException(WDErrorResponse)); // ✅ Werfe Exception
                    } else {
                        future.complete(responseType.cast(WDErrorResponse)); // ✅ Gib `ErrorResponse` als DTO zurück
                    }
                    WebSocketImpl.getInstance().offFrameReceived(listenerRef.get()); // Listener entfernen
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
                        WebSocketImpl.getInstance().offFrameReceived(listenerRef.get()); // Listener entfernen
                    }
                } else {
                    System.out.println("[DEBUG] Frame erfüllt Predicate NICHT! Ignoriert.");
                }
            } catch (JsonSyntaxException e) {
                System.out.println("[ERROR] JSON Parsing-Fehler: " + e.getMessage());
            }
        };

        listenerRef.set(listener); // 🛠 Hier wird die Variable gesetzt!
        WebSocketImpl.getInstance().onFrameReceived(listenerRef.get()); // ✅ Sicherstellen, dass `listener` registriert ist

        return future;
    }

    /**
     * Registriert einen Event-Listener, der auf eingehende Events reagiert.
     *
     * @param eventDispatcher Der EventDispatcher, der die Events verarbeitet.
     */
    public void registerEventListener(EventDispatcher eventDispatcher) {
        WebSocketImpl.getInstance().onFrameReceived(frame -> {
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
        return WebSocketImpl.getInstance().isConnected();
    }
}
