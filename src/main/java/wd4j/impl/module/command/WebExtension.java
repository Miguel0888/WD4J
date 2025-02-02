package wd4j.impl.module.command;

import wd4j.core.CommandImpl;
import wd4j.core.generic.Command;

public class WebExtension {

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