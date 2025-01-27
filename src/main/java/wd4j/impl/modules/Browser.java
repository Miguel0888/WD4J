package wd4j.impl.modules;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import wd4j.core.CommandImpl;
import wd4j.core.WebSocketConnection;
import wd4j.impl.generic.Command;
import wd4j.impl.generic.Module;
import wd4j.impl.generic.Type;

import java.util.ArrayList;
import java.util.List;

public class Browser implements Module {

    private final WebSocketConnection webSocketConnection;

    public BrowserClientWindow clientWindow;
    public BrowserClientWindowInfo clientWindowInfo;
    public BrowserUserContext userContext;
    public BrowserUserContextInfo userContextInfo;

    public Browser(WebSocketConnection webSocketConnection) {
        this.webSocketConnection = webSocketConnection;
    }

                   /**
     * Closes the browser.
     *
     * @throws RuntimeException if the close operation fails.
     */
    public void closeBrowser() {
        try {
            webSocketConnection.send(new CloseCommand());
            System.out.println("Browser closed successfully.");
        } catch (RuntimeException e) {
            System.out.println("Error closing browser: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Creates a new user context in the browser.
     *
     * @return The ID of the created user context.
     * @throws RuntimeException if the creation fails.
     */
    public String createUserContext() {
        try {
            String response = webSocketConnection.send(new CreateUserContextCommand());
            JsonObject jsonResponse = new Gson().fromJson(response, JsonObject.class);
            String contextId = jsonResponse.getAsJsonObject("result").get("context").getAsString();
            System.out.println("User context created: " + contextId);
            return contextId;
        } catch (RuntimeException e) {
            System.out.println("Error creating user context: " + e.getMessage());
            throw e;
        }
    }


    /**
     * Retrieves the client windows of the browser.
     *
     * @return A list of client window IDs.
     * @throws RuntimeException if the operation fails.
     */
    public List<String> getClientWindows() {
        try {
            String response = webSocketConnection.send(new GetClientWindowsCommand());
            JsonObject jsonResponse = new Gson().fromJson(response, JsonObject.class);
            JsonArray windows = jsonResponse.getAsJsonObject("result").getAsJsonArray("windows");
            List<String> windowIds = new ArrayList<>();
            windows.forEach(window -> windowIds.add(window.getAsString()));
            System.out.println("Client windows retrieved: " + windowIds);
            return windowIds;
        } catch (RuntimeException e) {
            System.out.println("Error retrieving client windows: " + e.getMessage());
            throw e;
        }
    }


    /**
     * Retrieves the user contexts available in the browser.
     *
     * @return A list of user context IDs.
     * @throws RuntimeException if the operation fails.
     */
    public List<String> getUserContexts() {
        try {
            String response = webSocketConnection.send(new GetUserContextsCommand());
            JsonObject jsonResponse = new Gson().fromJson(response, JsonObject.class);
            JsonArray contexts = jsonResponse.getAsJsonObject("result").getAsJsonArray("contexts");
            List<String> contextIds = new ArrayList<>();
            contexts.forEach(context -> contextIds.add(context.getAsString()));
            System.out.println("User contexts retrieved: " + contextIds);
            return contextIds;
        } catch (RuntimeException e) {
            System.out.println("Error retrieving user contexts: " + e.getMessage());
            throw e;
        }
    }


    /**
     * Removes a user context from the browser.
     *
     * @param contextId The ID of the user context to remove.
     * @throws RuntimeException if the removal fails.
     */
    public void removeUserContext(String contextId) {
        try {
            webSocketConnection.send(new RemoveUserContextCommand(contextId));
            System.out.println("User context removed: " + contextId);
        } catch (RuntimeException e) {
            System.out.println("Error removing user context: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Sets the state of a client window.
     *
     * @param clientWindowId The ID of the client window.
     * @param state          The state to set (e.g., "minimized", "maximized").
     * @throws RuntimeException if setting the state fails.
     */
    public void setClientWindowState(String clientWindowId, String state) {
        try {
            webSocketConnection.send(new SetClientWindowStateCommand(clientWindowId, state));
            System.out.println("Client window state set: " + clientWindowId + " -> " + state);
        } catch (RuntimeException e) {
            System.out.println("Error setting client window state: " + e.getMessage());
            throw e;
        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Types (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public class BrowserClientWindow {
        private final String id;

        public BrowserClientWindow(String id) {
            if (id == null || id.isEmpty()) {
                throw new IllegalArgumentException("ID must not be null or empty.");
            }
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    public class BrowserClientWindowInfo {
        private final String id;
        private final String state;

        public BrowserClientWindowInfo(String id, String state) {
            if (id == null || id.isEmpty()) {
                throw new IllegalArgumentException("ID must not be null or empty.");
            }
            if (state == null || state.isEmpty()) {
                throw new IllegalArgumentException("State must not be null or empty.");
            }
            this.id = id;
            this.state = state;
        }

        public String getId() {
            return id;
        }

        public String getState() {
            return state;
        }
    }

    public class BrowserUserContext {
        private final String id;

        public BrowserUserContext(String id) {
            if (id == null || id.isEmpty()) {
                throw new IllegalArgumentException("ID must not be null or empty.");
            }
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    public class BrowserUserContextInfo {
        private final String id;
        private final String type;

        public BrowserUserContextInfo(String id, String type) {
            if (id == null || id.isEmpty()) {
                throw new IllegalArgumentException("ID must not be null or empty.");
            }
            if (type == null || type.isEmpty()) {
                throw new IllegalArgumentException("Type must not be null or empty.");
            }
            this.id = id;
            this.type = type;
        }

        public String getId() {
            return id;
        }

        public String getType() {
            return type;
        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Events (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Commands (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class CloseCommand extends CommandImpl<CloseCommand.ParamsImpl> {

        public CloseCommand() {
            super("browser.close", new ParamsImpl());
        }

        public static class ParamsImpl implements Command.Params {
            // Keine Parameter erforderlich, daher bleibt die Klasse leer.
        }
    }


    public static class CreateUserContextCommand extends CommandImpl<CreateUserContextCommand.ParamsImpl> {

        public CreateUserContextCommand() {
            super("browser.createUserContext", new ParamsImpl());
        }

        public static class ParamsImpl implements Command.Params {
            // Keine Parameter erforderlich, daher bleibt die Klasse leer.
        }
    }


    public static class GetClientWindowsCommand extends CommandImpl<GetClientWindowsCommand.ParamsImpl> {

        public GetClientWindowsCommand() {
            super("browser.getClientWindows", new ParamsImpl());
        }

        public static class ParamsImpl implements Command.Params {
            // Keine Parameter erforderlich, daher bleibt die Klasse leer.
        }
    }


    public static class GetUserContextsCommand extends CommandImpl<GetUserContextsCommand.ParamsImpl> {

        public GetUserContextsCommand() {
            super("browser.getUserContexts", new ParamsImpl());
        }

        public static class ParamsImpl implements Command.Params {
            // Keine Parameter erforderlich, daher bleibt die Klasse leer.
        }
    }


    public static class RemoveUserContextCommand extends CommandImpl<RemoveUserContextCommand.ParamsImpl> {

        public RemoveUserContextCommand(String contextId) {
            super("browser.removeUserContext", new ParamsImpl(contextId));
        }

        public static class ParamsImpl implements Command.Params {
            private final String context;

            public ParamsImpl(String contextId) {
                if (contextId == null || contextId.isEmpty()) {
                    throw new IllegalArgumentException("Context ID must not be null or empty.");
                }
                this.context = contextId;
            }
        }
    }


    public static class SetClientWindowStateCommand extends CommandImpl<SetClientWindowStateCommand.ParamsImpl> {

        public SetClientWindowStateCommand(String clientWindowId, String state) {
            super("browser.setClientWindowState", new ParamsImpl(clientWindowId, state));
        }

        public static class ParamsImpl implements Command.Params {
            private final String clientWindowId;
            private final String state;

            public ParamsImpl(String clientWindowId, String state) {
                if (clientWindowId == null || clientWindowId.isEmpty()) {
                    throw new IllegalArgumentException("Client Window ID must not be null or empty.");
                }
                if (state == null || state.isEmpty()) {
                    throw new IllegalArgumentException("State must not be null or empty.");
                }
                this.clientWindowId = clientWindowId;
                this.state = state;
            }
        }
    }



}

