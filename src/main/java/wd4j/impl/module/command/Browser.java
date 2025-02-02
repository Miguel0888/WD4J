package wd4j.impl.module.command;

import wd4j.core.CommandImpl;
import wd4j.core.generic.Command;

public class Browser {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Commands (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class Close extends CommandImpl<Close.ParamsImpl> {

        public Close() {
            super("browser.close", new ParamsImpl());
        }

        public static class ParamsImpl implements Command.Params {
            // Keine Parameter erforderlich, daher bleibt die Klasse leer.
        }
    }


    public static class CreateUserContext extends CommandImpl<CreateUserContext.ParamsImpl> {

        public CreateUserContext() {
            super("browser.createUserContext", new ParamsImpl());
        }

        public static class ParamsImpl implements Command.Params {
            // Keine Parameter erforderlich, daher bleibt die Klasse leer.
        }
    }


    public static class GetClientWindows extends CommandImpl<GetClientWindows.ParamsImpl> {

        public GetClientWindows() {
            super("browser.getClientWindows", new ParamsImpl());
        }

        public static class ParamsImpl implements Command.Params {
            // Keine Parameter erforderlich, daher bleibt die Klasse leer.
        }
    }


    public static class GetUserContexts extends CommandImpl<GetUserContexts.ParamsImpl> {

        public GetUserContexts() {
            super("browser.getUserContexts", new ParamsImpl());
        }

        public static class ParamsImpl implements Command.Params {
            // Keine Parameter erforderlich, daher bleibt die Klasse leer.
        }
    }


    public static class RemoveUserContext extends CommandImpl<RemoveUserContext.ParamsImpl> {

        public RemoveUserContext(String contextId) {
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


    public static class SetClientWindowState extends CommandImpl<SetClientWindowState.ParamsImpl> {

        public SetClientWindowState(String clientWindowId, String state) {
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

