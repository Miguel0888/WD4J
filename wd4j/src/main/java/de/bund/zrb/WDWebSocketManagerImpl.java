package de.bund.zrb;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import de.bund.zrb.api.WebSocketFrame;
import de.bund.zrb.service.WDEventDispatcher;
import de.bund.zrb.support.mapping.GsonMapperFactory;
import de.bund.zrb.api.WDCommand;
import de.bund.zrb.api.WDCommandResponse;
import de.bund.zrb.websocket.WDErrorResponse;
import de.bund.zrb.api.WDWebSocketManager;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public class WDWebSocketManagerImpl implements WDWebSocketManager {
    private final Gson gson = GsonMapperFactory.getGson(); // ✅ Nutzt zentrale Fabrik

    private final WDWebSocketImpl webSocket; // ToDo: Should be WebSocket instead of WebSocketImpl

    // Retry-Regeln:
    private static final int MAX_RETRY_COUNT = 5;
    private static final long MAX_RETRY_WINDOW_MILLIS = 30_000L;

    // Dispatcher: key = commandId, value = callback for this command
    private final Map<Integer, Consumer<WDCommandResponse<?>>> responseDispatcher =
            new ConcurrentHashMap<Integer, Consumer<WDCommandResponse<?>>>();

    private volatile boolean dispatcherListenerRegistered = false;

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Deprecated // since WebSocketConnection should not be a singleton anymore?
    private static volatile WDWebSocketManagerImpl instance; // ToDo: Remove singleton pattern

    @Deprecated // since WebSocketConnection should not be a singleton anymore?
    public WDWebSocketManagerImpl(WDWebSocketImpl webSocket) {
        this.webSocket = webSocket;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Sendet einen Befehl über den WebSocket.
     *
     * @param command Das Command-Objekt, das gesendet werden soll.
     */
    public void send(WDCommand command) {
        if (webSocket.isClosed()) { // ToDo: Find a better solution, the problem is a new connection removes all listeners
            throw new RuntimeException("WebSocket connection is closed. Please reestablish the connection.");
//            this.webSocket = WebSocketImpl.getInstance();
//            registerEventListener(eventDispatcher);
        }
        String jsonCommand = gson.toJson(command);
        webSocket.send(jsonCommand); // Nachricht senden
    }

    /**
     * Sendet einen Befehl und wartet auf die Antwort.
     * receive nutzt den Dispatcher (Map<id, callback>), um die Antwort zu liefern.
     *
     * @param command      Der Befehl, der gesendet wird.
     * @param responseType Die Klasse des erwarteten DTOs.
     * @param <T>          Der Typ der Antwort.
     * @return Ein deserialisiertes DTO der Klasse `T`.
     */
    public <T> T sendAndWaitForResponse(final WDCommand command, final Type responseType) {
        final CompletableFuture<WDCommandResponse<?>> future = new CompletableFuture<WDCommandResponse<?>>();

        // Lambda, die auf die finale Antwort reagiert (wird von receive/Dispatcher aufgerufen)
        receive(command, new Consumer<WDCommandResponse<?>>() {
            @Override
            public void accept(WDCommandResponse<?> response) {
                if (!future.isDone()) {
                    future.complete(response);
                }
            }
        });

        // Befehl senden
        send(command);

        try {
            WDCommandResponse<?> response = future.get(30, TimeUnit.SECONDS);

            // Fehler: hier prüfen und ggf. Exception werfen
            if (response instanceof WDErrorResponse) {
                throw (WDErrorResponse) response;
            }

            // String-Spezialfall: gesamte Response als JSON zurückgeben
            if (responseType == String.class) {
                @SuppressWarnings("unchecked")
                T asString = (T) gson.toJson(response);
                return asString;
            }

            Object result = response.getResult();
            if (result == null) {
                throw new RuntimeException("Response does not contain a 'result' field.");
            }

            if (result instanceof JsonElement) {
                return gson.fromJson((JsonElement) result, responseType);
            }

            // Fallback: map result via JSON
            return gson.fromJson(gson.toJson(result), responseType);

        } catch (TimeoutException e) {
            responseDispatcher.remove(command.getId());
            throw new RuntimeException("Timeout while waiting for response.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            responseDispatcher.remove(command.getId());
            throw new RuntimeException("Interrupted while waiting for response.", e);
        } catch (ExecutionException e) {
            responseDispatcher.remove(command.getId());
            if (e.getCause() instanceof WDErrorResponse) {
                throw (WDErrorResponse) e.getCause();
            }
            throw new RuntimeException("Error while waiting for response.", e);
        }
    }

    /**
     * Registriert einen Callback in der Dispatcher-Map.
     * Der zentrale WebSocket-Listener ruft diesen Callback anhand der id auf.
     *
     * Retry-Verhalten:
     * - Wenn `type == "error"`:
     *   - Solange NICHT (RetryCount >= MAX_RETRY_COUNT UND Age >= MAX_RETRY_WINDOW_MILLIS):
     *     - incrementRetryCount
     *     - Command erneut senden
     *     - Callback NICHT aufrufen
     *   - Erst wenn beide Limits erreicht/überschritten sind:
     *     - WDErrorResponse an Callback liefern (final)
     *
     * @param command         Der zugehörige Command.
     * @param responseHandler Callback für finale Antwort (oder finalen Fehler).
     * @return Future, das mit der finalen WDCommandResponse abgeschlossen wird.
     */
    public CompletableFuture<WDCommandResponse<?>> receive(final WDCommand command,
                                                           final Consumer<WDCommandResponse<?>> responseHandler) {

        final int commandId = command.getId();
        final CompletableFuture<WDCommandResponse<?>> future = new CompletableFuture<WDCommandResponse<?>>();

        // Wrap callback, damit Retry-Logik zentral pro Command ausgeführt wird
        Consumer<WDCommandResponse<?>> dispatcherCallback = new Consumer<WDCommandResponse<?>>() {
            @Override
            public void accept(WDCommandResponse<?> response) {
                // Retry-Handling nur für Errors
                if (response instanceof WDErrorResponse) {
                    long now = System.currentTimeMillis();
                    long age = now - command.getFirstTimestamp();
                    int retries = command.getRetryCount();

                    boolean maxCountReached = retries >= MAX_RETRY_COUNT;
                    boolean maxAgeReached = age >= MAX_RETRY_WINDOW_MILLIS;

                    // Solange nicht beide Limits erreicht → retry und NICHT benachrichtigen
                    if (!(maxCountReached && maxAgeReached)) {
                        command.incrementRetryCount();
                        String retryJson = gson.toJson(command);
                        webSocket.send(retryJson);
                        // Ignore this error for listener and future
                        return;
                    }
                    // An dieser Stelle: finaler Fehler, weiter unten normal behandeln
                }

                // Finale Antwort oder finaler Fehler: Dispatcher-Aufräumen
                responseDispatcher.remove(commandId);

                if (responseHandler != null) {
                    responseHandler.accept(response);
                }

                if (!future.isDone()) {
                    future.complete(response);
                }
            }
        };

        // In Sprungtabelle eintragen
        responseDispatcher.put(commandId, dispatcherCallback);

        // Sicherstellen, dass der zentrale Listener registriert ist
        ensureDispatcherListenerRegistered();

        return future;
    }

    /**
     * Registriert einmalig einen zentralen Listener, der alle Frames entgegennimmt
     * und anhand der id an responseDispatcher verteilt.
     */
    private void ensureDispatcherListenerRegistered() {
        if (dispatcherListenerRegistered) {
            return;
        }
        synchronized (this) {
            if (dispatcherListenerRegistered) {
                return;
            }

            webSocket.onFrameReceived(new Consumer<WebSocketFrame>() {
                @Override
                public void accept(WebSocketFrame frame) {
                    try {
                        String text = frame.text();
                        JsonObject json = gson.fromJson(text, JsonObject.class);
                        if (json == null || !json.has("id")) {
                            // Events ohne id werden woanders behandelt (registerEventListener)
                            return;
                        }

                        int id = json.get("id").getAsInt();
                        Consumer<WDCommandResponse<?>> callback = responseDispatcher.get(id);
                        if (callback == null) {
                            // Kein wartender Empfänger für diese id
                            return;
                        }

                        WDCommandResponse<?> response;

                        if (json.has("type") && "error".equals(json.get("type").getAsString())) {
                            // Fehlerantwort
                            response = gson.fromJson(text, WDErrorResponse.class);
                        } else {
                            // Generische Response mit type/result
                            final String type = json.has("type") ? json.get("type").getAsString() : null;
                            final int responseId = id;
                            final JsonElement resultElement = json.get("result");

                            response = new WDCommandResponse<Object>() {
                                @Override
                                public String getType() {
                                    return type;
                                }

                                @Override
                                public int getId() {
                                    return responseId;
                                }

                                @Override
                                public Object getResult() {
                                    return resultElement;
                                }
                            };
                        }

                        // Übergabe an den für diese id registrierten Handler
                        callback.accept(response);

                    } catch (JsonSyntaxException e) {
                        System.out.println("[ERROR] JSON Parsing-Fehler: " + e.getMessage());
                    }
                }
            });

            dispatcherListenerRegistered = true;
        }
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
}
