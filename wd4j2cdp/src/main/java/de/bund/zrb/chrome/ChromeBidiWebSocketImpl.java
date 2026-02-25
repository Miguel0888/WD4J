package de.bund.zrb.chrome;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.bund.zrb.api.WDWebSocket;
import de.bund.zrb.api.WebSocketFrame;
import de.bund.zrb.chrome.cdp.CdpConnection;
import de.bund.zrb.chrome.cdp.CdpMapperSetup;

import java.net.URI;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * A WebSocket-like adapter that routes WebDriver BiDi messages through
 * Chrome's CDP via the chromium-bidi mapper.
 * <p>
 * This class implements the same interface as {@link de.bund.zrb.WDWebSocketImpl}
 * so it can be used as a drop-in replacement for Chrome support.
 * <p>
 * Protocol:
 * <ul>
 *   <li>Outgoing BiDi messages → sent via Runtime.evaluate("onBidiMessage(...)") on the mapper tab</li>
 *   <li>Incoming BiDi responses ← received via Runtime.bindingCalled event (name="sendBidiResponse")</li>
 * </ul>
 */
public class ChromeBidiWebSocketImpl implements WDWebSocket {
    private static final Gson GSON = new Gson();

    private final CdpConnection cdpConnection;
    private final CdpMapperSetup.MapperHandle mapperHandle;
    private final String url;
    private volatile boolean closed = false;

    private final List<Consumer<WDWebSocket>> onCloseListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<WebSocketFrame>> onFrameReceivedListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<WebSocketFrame>> onFrameSentListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<String>> onSocketErrorListeners = new CopyOnWriteArrayList<>();

    /**
     * Creates and initializes the Chrome BiDi adapter.
     *
     * @param cdpWebSocketUrl the CDP browser WebSocket URL (e.g. ws://127.0.0.1:PORT/devtools/browser/UUID)
     * @throws Exception if connection or mapper setup fails
     */
    public ChromeBidiWebSocketImpl(String cdpWebSocketUrl) throws Exception {
        this(cdpWebSocketUrl, false);
    }

    public ChromeBidiWebSocketImpl(String cdpWebSocketUrl, boolean verbose) throws Exception {
        this.url = cdpWebSocketUrl;
        System.out.println("[ChromeBidi] Connecting to CDP: " + cdpWebSocketUrl);

        // Step 1: Connect to CDP
        this.cdpConnection = new CdpConnection(new URI(cdpWebSocketUrl));

        // Step 2: Register event listener for BiDi responses BEFORE setting up the mapper
        cdpConnection.addEventListener("Runtime.bindingCalled", this::onBindingCalled);

        // Step 3: Setup the mapper
        this.mapperHandle = CdpMapperSetup.setupMapper(cdpConnection, verbose);

        // Step 4: Send session.new to initialize the mapper's target management.
        // Without this, the mapper doesn't call Target.setAutoAttach and
        // browsingContext.getTree will always return empty contexts.
        initBidiSession();

        System.out.println("[ChromeBidi] Ready! BiDi adapter operational via mapper tab.");
    }

    /**
     * Send a session.new BiDi command to the mapper.
     * This triggers the mapper's internal initialization (Target.setAutoAttach,
     * Target.setDiscoverTargets) so it starts tracking existing browser tabs.
     */
    private void initBidiSession() {
        String sessionNewCmd = "{\"id\":0,\"method\":\"session.new\",\"params\":{\"capabilities\":{}}}";

        final CompletableFuture<String> sessionResponse = new CompletableFuture<>();

        // Temporarily listen for the session.new response
        Consumer<WebSocketFrame> responseListener = frame -> {
            String text = frame.text();
            if (text.contains("\"id\":0") || text.contains("\"id\": 0")) {
                sessionResponse.complete(text);
            }
        };
        onFrameReceivedListeners.add(responseListener);

        try {
            // Send the command
            String escapedJson = GSON.toJson(sessionNewCmd);
            String expression = "onBidiMessage(" + escapedJson + ")";
            JsonObject evalParams = new JsonObject();
            evalParams.addProperty("expression", expression);
            cdpConnection.sendCommand("Runtime.evaluate", evalParams, mapperHandle.sessionId);

            // Wait for response (with timeout)
            String response = sessionResponse.get(15, TimeUnit.SECONDS);
            System.out.println("[ChromeBidi] session.new response: " +
                    (response.length() > 200 ? response.substring(0, 200) + "..." : response));

            // Give the mapper a moment to auto-attach to existing targets
            Thread.sleep(1000);
        } catch (Exception e) {
            System.err.println("[ChromeBidi] Warning: session.new failed: " + e.getMessage());
            System.err.println("[ChromeBidi] The mapper may not discover existing tabs.");
        } finally {
            onFrameReceivedListeners.remove(responseListener);
        }
    }

    // ── WDWebSocket interface ──

    @Override
    public void onClose(Consumer<WDWebSocket> handler) {
        onCloseListeners.add(handler);
    }

    @Override
    public void offClose(Consumer<WDWebSocket> handler) {
        onCloseListeners.remove(handler);
    }

    @Override
    public void onFrameReceived(Consumer<WebSocketFrame> handler) {
        onFrameReceivedListeners.add(handler);
    }

    @Override
    public void offFrameReceived(Consumer<WebSocketFrame> handler) {
        onFrameReceivedListeners.remove(handler);
    }

    @Override
    public void onFrameSent(Consumer<WebSocketFrame> handler) {
        onFrameSentListeners.add(handler);
    }

    @Override
    public void offFrameSent(Consumer<WebSocketFrame> handler) {
        onFrameSentListeners.remove(handler);
    }

    @Override
    public void onSocketError(Consumer<String> handler) {
        onSocketErrorListeners.add(handler);
    }

    @Override
    public void offSocketError(Consumer<String> handler) {
        onSocketErrorListeners.remove(handler);
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public String url() {
        return url;
    }

    // ── Send / close (same signatures as WDWebSocketImpl) ──

    /**
     * Send a BiDi JSON message to Chrome via the mapper.
     */
    public void send(String jsonBidiMessage) {
        if (closed) {
            throw new RuntimeException("Cannot send message: ChromeBidi adapter is closed.");
        }

        // Send via Runtime.evaluate on the mapper session
        // The mapper expects: onBidiMessage(jsonString)
        String escapedJson = GSON.toJson(jsonBidiMessage); // This creates a JSON string with proper escaping
        String expression = "onBidiMessage(" + escapedJson + ")";

        JsonObject evalParams = new JsonObject();
        evalParams.addProperty("expression", expression);

        cdpConnection.sendCommand("Runtime.evaluate", evalParams, mapperHandle.sessionId)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        System.err.println("[ChromeBidi] Error sending BiDi message: " + error.getMessage());
                        for (Consumer<String> listener : onSocketErrorListeners) {
                            listener.accept("Error sending BiDi message: " + error.getMessage());
                        }
                    }
                });

        // Notify frame-sent listeners
        WebSocketFrameImpl frame = new WebSocketFrameImpl(jsonBidiMessage);
        for (Consumer<WebSocketFrame> listener : onFrameSentListeners) {
            listener.accept(frame);
        }

        if (Boolean.getBoolean("wd4j.log.websocket")) {
            System.out.println("[ChromeBidi] >>> " + jsonBidiMessage);
        }
    }

    /**
     * Close the adapter: close mapper tab and CDP connection.
     */
    public void close() {
        if (closed) return;
        closed = true;

        // Try to clean up the mapper tab
        try {
            JsonObject closeParams = new JsonObject();
            closeParams.addProperty("targetId", mapperHandle.targetId);
            cdpConnection.sendCommand("Target.closeTarget", closeParams)
                    .get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.err.println("[ChromeBidi] Error closing mapper tab: " + e.getMessage());
        }

        cdpConnection.close();

        // Notify close listeners
        for (Consumer<WDWebSocket> listener : onCloseListeners) {
            listener.accept(this);
        }
    }

    public boolean isConnected() {
        return cdpConnection.isOpen() && !closed;
    }

    /**
     * Returns the underlying CDP connection. Useful for fallback operations
     * like querying existing targets when the BiDi mapper's getTree returns empty.
     */
    public CdpConnection getCdpConnection() {
        return cdpConnection;
    }

    /**
     * Returns the mapper handle (sessionId + targetId of the mapper tab).
     */
    public CdpMapperSetup.MapperHandle getMapperHandle() {
        return mapperHandle;
    }

    // ── Internal ──

    /**
     * Called when the mapper tab sends a BiDi response via Runtime.bindingCalled.
     */
    private void onBindingCalled(JsonObject params) {
        String name = params.has("name") ? params.get("name").getAsString() : "";

        if ("sendBidiResponse".equals(name)) {
            String payload = params.get("payload").getAsString();

            if (Boolean.getBoolean("wd4j.log.websocket")) {
                String logMsg = payload.length() > 500 ? payload.substring(0, 500) + "..." : payload;
                System.out.println("[ChromeBidi] <<< " + logMsg);
            }

            WebSocketFrameImpl frame = new WebSocketFrameImpl(payload);
            for (Consumer<WebSocketFrame> listener : onFrameReceivedListeners) {
                try {
                    listener.accept(frame);
                } catch (Exception e) {
                    System.err.println("[ChromeBidi] Error in frame listener: " + e.getMessage());
                }
            }
        } else if ("sendDebugMessage".equals(name)) {
            String payload = params.has("payload") ? params.get("payload").getAsString() : "";
            System.out.println("[ChromeBidi:debug] " + payload);
        }
    }

    // ── WebSocketFrame impl ──

    public static class WebSocketFrameImpl implements WebSocketFrame {
        private final String textData;

        WebSocketFrameImpl(String text) {
            this.textData = text;
        }

        @Override
        public byte[] binary() {
            return new byte[0];
        }

        @Override
        public String text() {
            return textData != null ? textData : "";
        }
    }
}

