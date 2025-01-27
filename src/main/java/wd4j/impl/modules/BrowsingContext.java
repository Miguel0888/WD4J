package wd4j.impl.modules;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import wd4j.core.CommandImpl;
import wd4j.core.WebSocketConnection;
import wd4j.impl.generic.Command;
import wd4j.impl.generic.Module;
import wd4j.impl.generic.Event;

import java.util.concurrent.ExecutionException;

public class BrowsingContext implements Module {

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// ToDo: Import the Specs: Commands (Methodes) and Types (Classes)
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final WebSocketConnection webSocketConnection;
    private final String contextId;

    public BrowsingContext(WebSocketConnection webSocketConnection) {
        this.webSocketConnection = webSocketConnection;
        this.contextId = createContext();
    }

    public BrowsingContext(WebSocketConnection webSocketConnection, String contextId) {
        this.webSocketConnection = webSocketConnection;
        this.contextId = contextId;
    }

    /*
     * Required for Firefox ESR ?
     */
    // Hilfsmethode: Neuen Context erstellen
    private String createContext() {
        CommandImpl createContextCommand = new CreateCommand("tab");

        try {
            String response = webSocketConnection.send(createContextCommand);

            JsonObject jsonResponse = new Gson().fromJson(response, JsonObject.class);
            JsonObject result = jsonResponse.getAsJsonObject("result");

            if (result != null && result.has("context")) {
                String contextId = result.get("context").getAsString();
                System.out.println("--- Neuer Context erstellt: " + contextId);
                return contextId;
            }
        } catch (RuntimeException e) {
            System.out.println("Error creating context: " + e.getMessage());
            throw e;
        }

        throw new IllegalStateException("Failed to create new context."); // ToDo: Maybe return null instead?
    }

    /**
     * Navigates to the given URL within this browsing context.
     *
     * @param url The target URL to navigate to.
     * @return The response of the navigation command.
     * @throws ExecutionException   if an error occurs during execution.
     * @throws InterruptedException if the operation is interrupted.
     */
    public void navigate(String url) throws ExecutionException, InterruptedException {
        // Send the command and wait for the response
        webSocketConnection.sendAsync(new NavigateCommand(url, contextId));
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Activates the given browsing context.
     *
     * @param contextId The ID of the context to activate.
     * @throws RuntimeException if the activation fails.
     */
    public void activate(String contextId) {
        try {
            webSocketConnection.send(new ActivateCommand(contextId));
        } catch (RuntimeException e) {
            System.out.println("Error activating context: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Captures a screenshot of the given browsing context.
     *
     * @param contextId The ID of the context to capture a screenshot from.
     * @return The screenshot as a base64-encoded string.
     */
    public String captureScreenshot(String contextId) {
        try {
            String response = webSocketConnection.send(new CaptureScreenshotCommand(contextId));
            JsonObject jsonResponse = new Gson().fromJson(response, JsonObject.class);
            return jsonResponse.getAsJsonObject("result").get("data").getAsString();
        } catch (RuntimeException e) {
            System.out.println("Error capturing screenshot: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Closes the given browsing context.
     *
     * @param contextId The ID of the context to close.
     * @throws RuntimeException if the close operation fails.
     */
    public void close(String contextId) {
        try {
            webSocketConnection.send(new CloseCommand(contextId));
        } catch (RuntimeException e) {
            System.out.println("Error closing context: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Handles a user prompt in the given browsing context.
     *
     * @param contextId The ID of the context where the prompt should be handled.
     * @param userText  The text to provide to the prompt, or null if no input is needed.
     * @throws RuntimeException if handling the prompt fails.
     */
    public void handleUserPrompt(String contextId, String userText) {
        try {
            webSocketConnection.send(new HandleUserPromptCommand(contextId, userText));
        } catch (RuntimeException e) {
            System.out.println("Error handling user prompt: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Locates nodes in the given browsing context using the provided CSS selector.
     *
     * @param contextId The ID of the context to search in.
     * @param selector  The CSS selector to locate nodes.
     * @return The response containing the located nodes.
     */
    public String locateNodes(String contextId, String selector) {
        try {
            String response = webSocketConnection.send(new LocateNodesCommand(contextId, selector));
            JsonObject jsonResponse = new Gson().fromJson(response, JsonObject.class);
            return jsonResponse.getAsJsonObject("result").toString();
        } catch (RuntimeException e) {
            System.out.println("Error locating nodes: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Prints the current page in the given browsing context.
     *
     * @param contextId The ID of the context to print.
     * @return The print output as a base64-encoded string.
     * @throws RuntimeException if the print operation fails.
     */
    public String print(String contextId) {
        try {
            String response = webSocketConnection.send(new PrintCommand(contextId));
            JsonObject jsonResponse = new Gson().fromJson(response, JsonObject.class);
            return jsonResponse.getAsJsonObject("result").get("data").getAsString();
        } catch (RuntimeException e) {
            System.out.println("Error printing context: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Reloads the given browsing context.
     *
     * @param contextId The ID of the context to reload.
     * @throws RuntimeException if the reload operation fails.
     */
    public void reload(String contextId) {
        try {
            webSocketConnection.send(new ReloadCommand(contextId));
        } catch (RuntimeException e) {
            System.out.println("Error reloading context: " + e.getMessage());
            throw e;
        }
    }


    /**
     * Sets the viewport size of the given browsing context.
     *
     * @param contextId The ID of the context to resize.
     * @param width     The new width of the viewport in pixels.
     * @param height    The new height of the viewport in pixels.
     * @throws RuntimeException if setting the viewport size fails.
     */
    public void setViewport(String contextId, int width, int height) {
        try {
            webSocketConnection.send(new SetViewportCommand(contextId, width, height));
        } catch (RuntimeException e) {
            System.out.println("Error setting viewport: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Traverses the browsing history in the given context by a specific delta.
     *
     * @param contextId The ID of the context to navigate.
     * @param delta     The number of steps to move in the history (e.g., -1 for back, 1 for forward).
     * @throws RuntimeException if traversing history fails.
     */
    public void traverseHistory(String contextId, int delta) {
        try {
            webSocketConnection.send(new TraverseHistoryCommand(contextId, delta));
        } catch (RuntimeException e) {
            System.out.println("Error traversing history: " + e.getMessage());
            throw e;
        }
    }


    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// ToDo: Commands (Methods)
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    // ToDo: Move create context from BiDiWebDriver to here!

    // public CompletableFuture<String> createContext() {
    //     JsonObject command = new JsonObject();
    //     command.addProperty("method", "browsingContext.create");
    //     return connection.sendAsync(command);
    // }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Events (Classes)
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    public static class CreatedEvent implements Event {
        private final JsonObject data;
        private final String type;
        private final String contextId;
        private final String url;

        public CreatedEvent(JsonObject json) {
            this.data = json;
            this.type = json.get("type").getAsString();
            this.contextId = json.getAsJsonObject("context").get("contextId").getAsString();
            this.url = json.getAsJsonObject("context").get("url").getAsString();
        }

        public String getContextId() {
            return contextId;
        }

        public String getUrl() {
            return url;
        }

        @Override
        public String toString() {
            return "BrowsingContext.CreatedEvent{contextId=" + contextId + ", url='" + url + "'}";
        }

        @Override
        public String getType() {
            return type;
        }

        @Override
        public JsonObject getData() {
            return data;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Commands (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class ActivateCommand extends CommandImpl<ActivateCommand.ParamsImpl> {

        public ActivateCommand(String contextId) {
            super("browsingContext.activate", new ParamsImpl(contextId));
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


    public static class CaptureScreenshotCommand extends CommandImpl<CaptureScreenshotCommand.ParamsImpl> {

        public CaptureScreenshotCommand(String contextId) {
            super("browsingContext.captureScreenshot", new ParamsImpl(contextId));
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


    public static class CloseCommand extends CommandImpl<CloseCommand.ParamsImpl> {

        public CloseCommand(String contextId) {
            super("browsingContext.close", new ParamsImpl(contextId));
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


    public static class CreateCommand extends CommandImpl<CreateCommand.ParamsImpl> {

        public CreateCommand(String type) {
            super("browsingContext.create", new ParamsImpl(type));
        }

        public static class ParamsImpl implements Command.Params {
            private final String type;

            public ParamsImpl(String type) {
                if (type == null || type.isEmpty()) {
                    throw new IllegalArgumentException("Type must not be null or empty.");
                }
                this.type = type;
            }
        }
    }

    public static class GetTreeCommand extends CommandImpl<GetTreeCommand.ParamsImpl> {

        public GetTreeCommand() {
            super("browsingContext.getTree", new ParamsImpl());
        }

        public static class ParamsImpl implements Params {
            // Keine Parameter erforderlich, daher bleibt die Klasse leer.
        }
    }

    public static class HandleUserPromptCommand extends CommandImpl<HandleUserPromptCommand.ParamsImpl> {

        public HandleUserPromptCommand(String contextId, String userText) {
            super("browsingContext.handleUserPrompt", new ParamsImpl(contextId, userText));
        }

        public static class ParamsImpl implements Command.Params {
            private final String context;
            private final String userText;

            public ParamsImpl(String contextId, String userText) {
                if (contextId == null || contextId.isEmpty()) {
                    throw new IllegalArgumentException("Context ID must not be null or empty.");
                }
                this.context = contextId;
                this.userText = userText; // `userText` kann null sein, falls der Nutzer keinen Text eingibt.
            }
        }
    }

    public static class LocateNodesCommand extends CommandImpl<LocateNodesCommand.ParamsImpl> {

        public LocateNodesCommand(String contextId, String selector) {
            super("browsingContext.locateNodes", new ParamsImpl(contextId, selector));
        }

        public static class ParamsImpl implements Command.Params {
            private final String context;
            private final String selector;

            public ParamsImpl(String contextId, String selector) {
                if (contextId == null || contextId.isEmpty()) {
                    throw new IllegalArgumentException("Context ID must not be null or empty.");
                }
                if (selector == null || selector.isEmpty()) {
                    throw new IllegalArgumentException("Selector must not be null or empty.");
                }
                this.context = contextId;
                this.selector = selector;
            }
        }
    }


    public static class NavigateCommand extends CommandImpl<NavigateCommand.ParamsImpl> {

        public NavigateCommand(String url, String contextId) {
            super("browsingContext.navigate", new ParamsImpl(url, contextId));
        }

        public static class ParamsImpl implements Command.Params {
            private final String url;
            private final String context;

            public ParamsImpl(String url, String contextId) {
                if (url == null || url.isEmpty()) {
                    throw new IllegalArgumentException("URL must not be null or empty.");
                }
                if (contextId == null || contextId.isEmpty()) {
                    throw new IllegalArgumentException("Context ID must not be null or empty.");
                }
                this.url = url;
                this.context = contextId;
            }
        }
    }

    public static class PrintCommand extends CommandImpl<PrintCommand.ParamsImpl> {

        public PrintCommand(String contextId) {
            super("browsingContext.print", new ParamsImpl(contextId));
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


    public static class ReloadCommand extends CommandImpl<ReloadCommand.ParamsImpl> {

        public ReloadCommand(String contextId) {
            super("browsingContext.reload", new ParamsImpl(contextId));
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


    public static class SetViewportCommand extends CommandImpl<SetViewportCommand.ParamsImpl> {

        public SetViewportCommand(String contextId, int width, int height) {
            super("browsingContext.setViewport", new ParamsImpl(contextId, width, height));
        }

        public static class ParamsImpl implements Command.Params {
            private final String context;
            private final int width;
            private final int height;

            public ParamsImpl(String contextId, int width, int height) {
                if (contextId == null || contextId.isEmpty()) {
                    throw new IllegalArgumentException("Context ID must not be null or empty.");
                }
                if (width <= 0) {
                    throw new IllegalArgumentException("Width must be greater than 0.");
                }
                if (height <= 0) {
                    throw new IllegalArgumentException("Height must be greater than 0.");
                }
                this.context = contextId;
                this.width = width;
                this.height = height;
            }
        }
    }


    public static class TraverseHistoryCommand extends CommandImpl<TraverseHistoryCommand.ParamsImpl> {

        public TraverseHistoryCommand(String contextId, int delta) {
            super("browsingContext.traverseHistory", new ParamsImpl(contextId, delta));
        }

        public static class ParamsImpl implements Command.Params {
            private final String context;
            private final int delta;

            public ParamsImpl(String contextId, int delta) {
                if (contextId == null || contextId.isEmpty()) {
                    throw new IllegalArgumentException("Context ID must not be null or empty.");
                }
                this.context = contextId;
                this.delta = delta;
            }
        }
    }



}