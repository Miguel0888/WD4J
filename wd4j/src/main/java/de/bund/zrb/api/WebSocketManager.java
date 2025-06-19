package de.bund.zrb.api;

import de.bund.zrb.EventDispatcher;

import java.lang.reflect.Type;

public interface WebSocketManager {

    /**
     * Send the given command and wait for a typed response.
     *
     * @param command the command object to send
     * @param responseType the expected response type (can be Class<T> or any Type)
     * @param <T> the actual result object inside the WebDriver response wrapper
     * @return deserialized result object
     */
    <T> T sendAndWaitForResponse(WDCommand command, Type responseType);

    void registerEventListener(EventDispatcher eventDispatcher);

    /**
     * Returns true if the underlying WebSocket is connected.
     *
     * @return true if connected
     */
    boolean isConnected();
}
