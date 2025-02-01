package wd4j.impl.modules;

import com.google.gson.JsonObject;
import wd4j.core.CommandImpl;
import wd4j.core.WebSocketConnection;
import wd4j.impl.generic.Command;
import wd4j.impl.generic.Module;

import java.util.List;

public class InputService implements Module {

    private final WebSocketConnection webSocketConnection;

    public InputService(WebSocketConnection webSocketConnection) {
        this.webSocketConnection = webSocketConnection;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

                 /**
     * Performs a sequence of input actions in the given browsing context.
     *
     * @param contextId The ID of the context where the actions are performed.
     * @param actions   A list of actions to perform.
     * @throws RuntimeException if the action execution fails.
     */
    public void performActions(String contextId, List<PerformActions.Action> actions) {
        if (actions == null || actions.isEmpty()) {
            throw new IllegalArgumentException("Actions list must not be null or empty.");
        }

        try {
            webSocketConnection.send(new PerformActions(contextId, actions));
            System.out.println("Performed actions in context: " + contextId);
        } catch (RuntimeException e) {
            System.out.println("Error performing actions: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Releases all input actions for the given browsing context.
     *
     * @param contextId The ID of the context where the actions are released.
     * @throws RuntimeException if the release operation fails.
     */
    public void releaseActions(String contextId) {
        try {
            webSocketConnection.send(new ReleaseActions(contextId));
            System.out.println("Released actions in context: " + contextId);
        } catch (RuntimeException e) {
            System.out.println("Error releasing actions: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Sets files to the given input element.
     *
     * @param elementId The ID of the input element.
     * @param filePaths A list of file paths to set.
     * @throws RuntimeException if the operation fails.
     */
    public void setFiles(String elementId, List<String> filePaths) {
        if (filePaths == null || filePaths.isEmpty()) {
            throw new IllegalArgumentException("File paths list must not be null or empty.");
        }

        try {
            webSocketConnection.send(new SetFiles(elementId, filePaths));
            System.out.println("Files set for element: " + elementId);
        } catch (RuntimeException e) {
            System.out.println("Error setting files: " + e.getMessage());
            throw e;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Types (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public class InputOrigin {

        private final String type;
        private final String elementId;

        /**
         * Constructor for InputOrigin with a specific type.
         *
         * @param type The type of origin (e.g., "viewport", "pointer").
         */
        public InputOrigin(String type) {
            if (type == null || type.isEmpty()) {
                throw new IllegalArgumentException("Type must not be null or empty.");
            }
            if (!type.equals("viewport") && !type.equals("pointer")) {
                throw new IllegalArgumentException("Invalid type for InputOrigin: " + type);
            }
            this.type = type;
            this.elementId = null;
        }

        /**
         * Constructor for InputOrigin with an element reference.
         *
         * @param elementId The ID of the element to use as the origin.
         */
        public InputOrigin(String elementId, boolean isElement) {
            if (elementId == null || elementId.isEmpty()) {
                throw new IllegalArgumentException("Element ID must not be null or empty.");
            }
            this.type = "element";
            this.elementId = elementId;
        }

        /**
         * Serializes the InputOrigin into a JSON object.
         *
         * @return A JSON object representing the InputOrigin.
         */
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", type);
            if ("element".equals(type)) {
                json.addProperty("element", elementId);
            }
            return json;
        }

        // Getters for type and elementId
        public String getType() {
            return type;
        }

        public String getElementId() {
            return elementId;
        }

        @Override
        public String toString() {
            return toJson().toString();
        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Events (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Commands (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class PerformActions extends CommandImpl<PerformActions.ParamsImpl> {

        public PerformActions(String contextId, List<Action> actions) {
            super("input.performActions", new ParamsImpl(contextId, actions));
        }

        public static class ParamsImpl implements Command.Params {
            private final String context;
            private final List<Action> actions;

            public ParamsImpl(String contextId, List<Action> actions) {
                if (contextId == null || contextId.isEmpty()) {
                    throw new IllegalArgumentException("Context ID must not be null or empty.");
                }
                if (actions == null || actions.isEmpty()) {
                    throw new IllegalArgumentException("Actions list must not be null or empty.");
                }
                this.context = contextId;
                this.actions = actions;
            }
        }

        public static class Action {
            private final String type;
            private final String id;

            public Action(String type, String id) {
                this.type = type;
                this.id = id;
            }
        }
    }

    public static class ReleaseActions extends CommandImpl<ReleaseActions.ParamsImpl> {

        public ReleaseActions(String contextId) {
            super("input.releaseActions", new ParamsImpl(contextId));
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

    public static class SetFiles extends CommandImpl<SetFiles.ParamsImpl> {

        public SetFiles(String elementId, List<String> filePaths) {
            super("input.setFiles", new ParamsImpl(elementId, filePaths));
        }

        public static class ParamsImpl implements Command.Params {
            private final String element;
            private final List<String> files;

            public ParamsImpl(String elementId, List<String> filePaths) {
                if (elementId == null || elementId.isEmpty()) {
                    throw new IllegalArgumentException("Element ID must not be null or empty.");
                }
                if (filePaths == null || filePaths.isEmpty()) {
                    throw new IllegalArgumentException("File paths list must not be null or empty.");
                }
                this.element = elementId;
                this.files = filePaths;
            }
        }
    }

}