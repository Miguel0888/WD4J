package wd4j.impl.modules;

import wd4j.core.CommandImpl;
import wd4j.core.WebSocketConnection;
import wd4j.impl.generic.Command;
import wd4j.impl.generic.Module;
import wd4j.impl.generic.Type;

public class WebExtension implements Module {

    public Extension extension;

    private final WebSocketConnection webSocketConnection;

    public WebExtension(WebSocketConnection webSocketConnection) {
        this.webSocketConnection = webSocketConnection;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Installs a web extension in the specified browsing context.
     *
     * @param contextId     The ID of the browsing context.
     * @param extensionPath The path to the extension to install.
     * @throws RuntimeException if the installation fails.
     */
    public void install(String contextId, String extensionPath) {
        try {
            webSocketConnection.send(new Install(contextId, extensionPath));
            System.out.println("Web extension installed from path: " + extensionPath + " in context: " + contextId);
        } catch (RuntimeException e) {
            System.out.println("Error installing web extension: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Uninstalls a web extension from the specified browsing context.
     *
     * @param contextId   The ID of the browsing context.
     * @param extensionId The ID of the extension to uninstall.
     * @throws RuntimeException if the uninstallation fails.
     */
    public void uninstall(String contextId, String extensionId) {
        try {
            webSocketConnection.send(new Uninstall(contextId, extensionId));
            System.out.println("Web extension uninstalled: " + extensionId + " from context: " + contextId);
        } catch (RuntimeException e) {
            System.out.println("Error uninstalling web extension: " + e.getMessage());
            throw e;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Types (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class Extension implements Type {
        private final String id;
        private final String name;
        private final boolean enabled;
        private final String version;

        /**
         * Constructor for the WebExtension.Extension type.
         *
         * @param id      The unique ID of the extension.
         * @param name    The name of the extension.
         * @param enabled Whether the extension is enabled.
         * @param version The version of the extension.
         */
        public Extension(String id, String name, boolean enabled, String version) {
            if (id == null || id.isEmpty()) {
                throw new IllegalArgumentException("Extension ID must not be null or empty.");
            }
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("Extension name must not be null or empty.");
            }
            if (version == null || version.isEmpty()) {
                throw new IllegalArgumentException("Extension version must not be null or empty.");
            }
            this.id = id;
            this.name = name;
            this.enabled = enabled;
            this.version = version;
        }

        // Getters for all fields

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public String getVersion() {
            return version;
        }

        @Override
        public String toString() {
            return "Extension{" +
                    "id='" + id + '\'' +
                    ", name='" + name + '\'' +
                    ", enabled=" + enabled +
                    ", version='" + version + '\'' +
                    '}';
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Events (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Commands (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class Install extends CommandImpl<Install.ParamsImpl> {

        public Install(String contextId, String extensionPath) {
            super("webExtension.install", new ParamsImpl(contextId, extensionPath));
        }

        public static class ParamsImpl implements Command.Params {
            private final String context;
            private final String path;

            public ParamsImpl(String contextId, String extensionPath) {
                if (contextId == null || contextId.isEmpty()) {
                    throw new IllegalArgumentException("Context ID must not be null or empty.");
                }
                if (extensionPath == null || extensionPath.isEmpty()) {
                    throw new IllegalArgumentException("Extension path must not be null or empty.");
                }
                this.context = contextId;
                this.path = extensionPath;
            }
        }
    }

    public static class Uninstall extends CommandImpl<Uninstall.ParamsImpl> {

        public Uninstall(String contextId, String extensionId) {
            super("webExtension.uninstall", new ParamsImpl(contextId, extensionId));
        }

        public static class ParamsImpl implements Command.Params {
            private final String context;
            private final String extension;

            public ParamsImpl(String contextId, String extensionId) {
                if (contextId == null || contextId.isEmpty()) {
                    throw new IllegalArgumentException("Context ID must not be null or empty.");
                }
                if (extensionId == null || extensionId.isEmpty()) {
                    throw new IllegalArgumentException("Extension ID must not be null or empty.");
                }
                this.context = contextId;
                this.extension = extensionId;
            }
        }
    }

}