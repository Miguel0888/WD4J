package wd4j.impl.playwright.testing;

import com.microsoft.WebSocketRoute;
import com.microsoft.WebSocketFrame;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * NOT IMPLEMENTED YET
 */
// ToDo: Maybe used in WebSocketManager or WebSocketImpl to enable testing of WebSocket communication
public class WebSocketRouteImpl implements WebSocketRoute {
    /**
     * Closes one side of the WebSocket connection.
     *
     * @param options
     * @since v1.48
     */
    @Override
    public void close(CloseOptions options) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * By default, routed WebSocket does not connect to the server, so you can mock entire WebSocket communication. This method
     * connects to the actual WebSocket server, and returns the server-side {@code WebSocketRoute} instance, giving the ability
     * to send and receive messages from the server.
     *
     * <p> Once connected to the server:
     * <ul>
     * <li> Messages received from the server will be **automatically forwarded** to the WebSocket in the page, unless {@link
     * WebSocketRouteImpl#onMessage WebSocketRoute.onMessage()} is called on the server-side {@code
     * WebSocketRoute}.</li>
     * <li> Messages sent by the <a href="https://developer.mozilla.org/en-US/docs/Web/API/WebSocket/send">{@code
     * WebSocket.send()}</a> call in the page will be **automatically forwarded** to the server, unless {@link
     * WebSocketRouteImpl#onMessage WebSocketRoute.onMessage()} is called on the original {@code
     * WebSocketRoute}.</li>
     * </ul>
     *
     * <p> See examples at the top for more details.
     *
     * @since v1.48
     */
    @Override
    public WebSocketRoute connectToServer() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Allows to handle <a href="https://developer.mozilla.org/en-US/docs/Web/API/WebSocket/close">{@code WebSocket.close}</a>.
     *
     * <p> By default, closing one side of the connection, either in the page or on the server, will close the other side. However,
     * when {@link WebSocketRouteImpl#onClose WebSocketRoute.onClose()} handler is set up, the default
     * forwarding of closure is disabled, and handler should take care of it.
     *
     * @param handler Function that will handle WebSocket closure. Received an optional <a
     *                href="https://developer.mozilla.org/en-US/docs/Web/API/WebSocket/close#code">close code</a> and an optional <a
     *                href="https://developer.mozilla.org/en-US/docs/Web/API/WebSocket/close#reason">close reason</a>.
     * @since v1.48
     */
    @Override
    public void onClose(BiConsumer<Integer, String> handler) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * This method allows to handle messages that are sent by the WebSocket, either from the page or from the server.
     *
     * <p> When called on the original WebSocket route, this method handles messages sent from the page. You can handle this
     * messages by responding to them with {@link WebSocketRouteImpl#send WebSocketRoute.send()},
     * forwarding them to the server-side connection returned by {@link WebSocketRouteImpl#connectToServer
     * WebSocketRoute.connectToServer()} or do something else.
     *
     * <p> Once this method is called, messages are not automatically forwarded to the server or to the page - you should do that
     * manually by calling {@link WebSocketRouteImpl#send WebSocketRoute.send()}. See examples at the top
     * for more details.
     *
     * <p> Calling this method again will override the handler with a new one.
     *
     * @param handler Function that will handle messages.
     * @since v1.48
     */
    @Override
    public void onMessage(Consumer<WebSocketFrame> handler) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Sends a message to the WebSocket. When called on the original WebSocket, sends the message to the page. When called on
     * the result of {@link WebSocketRouteImpl#connectToServer WebSocketRoute.connectToServer()}, sends
     * the message to the server. See examples at the top for more details.
     *
     * @param message Message to send.
     * @since v1.48
     */
    @Override
    public void send(String message) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Sends a message to the WebSocket. When called on the original WebSocket, sends the message to the page. When called on
     * the result of {@link WebSocketRouteImpl#connectToServer WebSocketRoute.connectToServer()}, sends
     * the message to the server. See examples at the top for more details.
     *
     * @param message Message to send.
     * @since v1.48
     */
    @Override
    public void send(byte[] message) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * URL of the WebSocket created in the page.
     *
     * @since v1.48
     */
    @Override
    public String url() {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
