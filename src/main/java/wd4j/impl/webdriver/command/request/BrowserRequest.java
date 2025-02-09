package wd4j.impl.webdriver.command.request;

import wd4j.impl.markerInterfaces.CommandData;
import wd4j.impl.websocket.Command;

public class BrowserRequest {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Commands (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class Close extends CommandImpl<Close.ParamsImpl> implements CommandData {

        public Close() {
            super("browser.close", new ParamsImpl());
        }

        public static class ParamsImpl implements Command.Params {
            // Keine Parameter erforderlich, daher bleibt die Klasse leer.
        }
    }


    public static class CreateUserContext extends CommandImpl<CreateUserContext.ParamsImpl> implements CommandData {

        public CreateUserContext() {
            super("browser.createUserContext", new ParamsImpl());
        }

        public static class ParamsImpl implements Command.Params {
            // Keine Parameter erforderlich, daher bleibt die Klasse leer.
        }
    }


    public static class GetClientWindows extends CommandImpl<GetClientWindows.ParamsImpl> implements CommandData {

        public GetClientWindows() {
            super("browser.getClientWindows", new ParamsImpl());
        }

        public static class ParamsImpl implements Command.Params {
            // Keine Parameter erforderlich, daher bleibt die Klasse leer.
        }
    }


    public static class GetUserContexts extends CommandImpl<GetUserContexts.ParamsImpl> implements CommandData {

        public GetUserContexts() {
            super("browser.getUserContexts", new ParamsImpl());
        }

        public static class ParamsImpl implements Command.Params {
            // Keine Parameter erforderlich, daher bleibt die Klasse leer.
        }
    }


    public static class RemoveUserContext extends CommandImpl<RemoveUserContext.ParamsImpl> implements CommandData {

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


    public static class SetClientWindowState extends CommandImpl<SetClientWindowState.ParamsImpl> implements CommandData {

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

