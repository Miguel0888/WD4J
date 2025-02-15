package wd4j.impl.websocket;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import wd4j.api.WebSocket;
import wd4j.impl.playwright.WebSocketImpl;
import wd4j.impl.websocket.Command;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Implements the high-level communication with the browser. It delivers only WebDriver DTOs.
 * The low-level frame API is encapsulated.
 */
public class CommunicationManager {
    private final WebSocketImpl webSocketImpl;
    private final Gson gson = new Gson();

    public CommunicationManager(WebSocketImpl webSocketImpl) {
        this.webSocketImpl = webSocketImpl;
    }

    /**
     * Sends a command and returns a CompletableFuture to allow non-blocking operation.
     * @param command The command DTO to send.
     * @return CompletableFuture for the response as raw JSON string.
     */
    public CompletableFuture<String> send(Command command) {
        return webSocketImpl.send(command).thenApply(WebSocketFrame::text);
    }

    /**
     * Receives a response and maps it to the given DTO class.
     * @param response The raw JSON response.
     * @param responseType The class of the expected DTO.
     * @param <T> The expected DTO type.
     * @return The mapped DTO instance.
     */
    public <T> T receive(String response, Class<T> responseType) {
        return gson.fromJson(response, responseType);
    }

    /**
     * Sends a command and waits for the response, mapping it to a DTO.
     * @param command The command DTO to send.
     * @param responseType The class of the expected response DTO.
     * @param <T> The expected DTO type.
     * @return The mapped DTO instance.
     */
    public <T> T sendAndWaitForResponse(Command command, Class<T> responseType) {
        try {
            String jsonResponse = send(command).get(30, TimeUnit.SECONDS);
            return receive(jsonResponse, responseType);
        } catch (Exception e) {
            throw new RuntimeException("Error waiting for response", e);
        }
    }

    public boolean isConnected() {
        return webSocketImpl.isConnected();
    }
}
