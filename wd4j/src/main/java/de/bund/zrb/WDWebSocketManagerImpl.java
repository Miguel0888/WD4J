package de.bund.zrb;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import de.bund.zrb.api.WebSocketFrame;
import de.bund.zrb.service.WDEventDispatcher;
import de.bund.zrb.support.mapping.GsonMapperFactory;
import de.bund.zrb.api.WDCommand;
import de.bund.zrb.websocket.WDErrorResponse;
import de.bund.zrb.api.WDWebSocketManager;

import java.lang.reflect.Type;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * ToDo: WebSockeFrames wurden hier fälschlicherweise eingesetzt. Sie sind für Verbindungen der Seite selbst gedacht,
 * nicht für DIE verbindung zumn Browser über WebDriverBidi & WebSocket
 *
 */
public class WDWebSocketManagerImpl implements WDWebSocketManager {
    private static final long ERROR_RETRY_TIMEOUT_MILLIS = 30_000L;

    // Single scheduler for all timeouts (Java 8 kompatibel)
    private static final ScheduledExecutorService ERROR_TIMEOUT_SCHEDULER =
            Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "wd4j-ws-error-timeout");
                    t.setDaemon(true);
                    return t;
                }
            });

    private final Gson gson = GsonMapperFactory.getGson(); // ✅ Nutzt zentrale Fabrik

    private final WDWebSocketImpl webSocket; // ToDo: Should be WebSocket instead of WebSocketImpl

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Deprecated // since WebSocketConnection should not be a singleton anymore?
    private static volatile WDWebSocketManagerImpl instance; // ToDo: Remove singleton pattern

    @Deprecated // since WebSocketConnection should not be a singleton anymore?
    public WDWebSocketManagerImpl(WDWebSocketImpl webSocket) {
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
            if(e.getCause() instanceof WDErrorResponse)
            {
                throw (WDErrorResponse) e.getCause();
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
     * @throws WDErrorResponse Falls eine Fehlerantwort empfangen wird.
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
     * @throws WDErrorResponse Falls `throwError == true` und eine Fehlerantwort empfangen wird.
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
                    WDErrorResponse error = gson.fromJson(frame.text(), WDErrorResponse.class);

                    if (!throwError) {
                        // Caller will Fehlerobjekt direkt, kein Retry
                        completeOnce(future, responseType.cast(error), listenerRef, timeoutHandleRef);
                        return;
                    }

                    // Nur beim ersten Error das Timeout planen
                    if (firstErrorRef.compareAndSet(null, error)) {
                        ScheduledFuture<?> timeoutFuture = ERROR_TIMEOUT_SCHEDULER.schedule(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        if (!future.isDone()) {
                                            future.completeExceptionally(firstErrorRef.get());
                                            removeListener(listenerRef);
                                        }
                                    }
                                },
                                ERROR_RETRY_TIMEOUT_MILLIS,
                                TimeUnit.MILLISECONDS
                        );
                        timeoutHandleRef.set(timeoutFuture);
                    }

                    // Innerhalb der 30 Sekunden weiter auf einen gültigen Response warten
                    return;
                }

                // 2) Fachliche Fehler aus Skript / CallFunction -> sofort durchreichen
                if ("exception".equals(type) && throwError) {
                    WDErrorResponse error = gson.fromJson(text, WDErrorResponse.class);
                    future.completeExceptionally(error);
                    removeListener(listenerRef);
                    cancelTimeout(timeoutHandleRef.get());
                    return;
                }

                //  3) Falls Predicate erfüllt → Antwort parsen
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
    public void registerEventListener(WDEventDispatcher eventDispatcher) {
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

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // HELPER:
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private <T> void completeOnce(CompletableFuture<T> future,
                                  T value,
                                  AtomicReference<Consumer<WebSocketFrame>> listenerRef,
                                  AtomicReference<ScheduledFuture<?>> timeoutHandleRef) {
        if (future.complete(value)) {
            removeListener(listenerRef);
            cancelTimeout(timeoutHandleRef.get());
        }
    }

    private void removeListener(AtomicReference<Consumer<WebSocketFrame>> listenerRef) {
        Consumer<WebSocketFrame> l = listenerRef.get();
        if (l != null) {
            webSocket.offFrameReceived(l);
        }
    }

    private void cancelTimeout(ScheduledFuture<?> f) {
        if (f != null) {
            f.cancel(false);
        }
    }
}
