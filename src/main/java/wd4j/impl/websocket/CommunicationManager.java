package wd4j.impl.websocket;

import wd4j.api.WebSocket;
import wd4j.impl.playwright.WebSocketImpl;

/**
 * Implements the high-level communication with the browser. It delivers only WebDriver DTOs. Therefore, the low-level
 * frame api is encapsulated.
 *
 */
public class CommunicationManager {
    private final WebSocketImpl webSocketImpl;



    public CommunicationManager(WebSocketImpl webSocketImpl) {
        this.webSocketImpl = webSocketImpl;
    }

    /**
     * Returns the WebSocket object for Playwright compatibility.
     *
     * @return A WebSocket object.
     */
    public WebSocketImpl getWebSocket() {
        return webSocketImpl;
    }

    public boolean isConnected() {
        return webSocketImpl.isConnected();
    }
}
