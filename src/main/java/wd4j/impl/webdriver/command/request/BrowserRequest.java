package wd4j.impl.webdriver.command.request;

import wd4j.impl.markerInterfaces.CommandData;
import wd4j.impl.webdriver.command.request.parameters.browser.RemoveUserContextParameters;
import wd4j.impl.webdriver.command.request.parameters.browser.SetClientWindowStateParameters;
import wd4j.impl.webdriver.type.browser.ClientWindow;
import wd4j.impl.webdriver.type.browser.UserContext;
import wd4j.impl.websocket.Command;

public class BrowserRequest {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Commands (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class Close extends CommandImpl<EmptyParameters> implements CommandData {
        public Close() {
            super("browser.close", new EmptyParameters());
        }
    }

    public static class CreateUserContext extends CommandImpl<EmptyParameters> implements CommandData {
        public CreateUserContext() {
            super("browser.createUserContext", new EmptyParameters());
        }
    }

    public static class GetClientWindows extends CommandImpl<EmptyParameters> implements CommandData {
        public GetClientWindows() {
            super("browser.getClientWindows", new EmptyParameters());
        }
    }

    public static class GetUserContexts extends CommandImpl<EmptyParameters> implements CommandData {
        public GetUserContexts() {
            super("browser.getUserContexts", new EmptyParameters());
        }
    }

    public static class RemoveUserContext extends CommandImpl<RemoveUserContextParameters> implements CommandData {
        public RemoveUserContext(String contextId) {
            super("browser.removeUserContext", new RemoveUserContextParameters(new UserContext(contextId)));
        }

        public RemoveUserContext(UserContext context) {
            super("browser.removeUserContext", new RemoveUserContextParameters(context));
        }
    }

    public static class SetClientWindowState extends CommandImpl<SetClientWindowStateParameters<SetClientWindowStateParameters.ClientWindowState>> implements CommandData {
        public SetClientWindowState(String clientWindowId, String state) {
            super("browser.setClientWindowState", new SetClientWindowStateParameters<>(
                    new ClientWindow(clientWindowId),
                    new SetClientWindowStateParameters.ClientWindowNamedState(
                            SetClientWindowStateParameters.ClientWindowNamedState.State.valueOf(state))));
        }
        public SetClientWindowState(ClientWindow clientWindow, SetClientWindowStateParameters.ClientWindowState state) {
            super("browser.setClientWindowState", new SetClientWindowStateParameters<>(clientWindow, state));
        }
    }

}

