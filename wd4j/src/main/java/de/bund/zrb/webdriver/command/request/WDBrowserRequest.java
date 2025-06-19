package de.bund.zrb.webdriver.command.request;

import de.bund.zrb.markerInterfaces.WDCommandData;
import de.bund.zrb.webdriver.command.request.helper.WDCommandImpl;
import de.bund.zrb.webdriver.command.request.helper.WDEmptyParameters;
import de.bund.zrb.webdriver.command.request.parameters.browser.RemoveUserContextParameters;
import de.bund.zrb.webdriver.command.request.parameters.browser.SetClientWindowStateParameters;
import de.bund.zrb.webdriver.type.browser.WDClientWindow;
import de.bund.zrb.webdriver.type.browser.WDUserContext;

public class WDBrowserRequest {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Commands (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class Close extends WDCommandImpl<WDEmptyParameters> implements WDCommandData {
        public Close() {
            super("browser.close", new WDEmptyParameters());
        }
    }

    public static class CreateUserContext extends WDCommandImpl<WDEmptyParameters> implements WDCommandData {
        public CreateUserContext() {
            super("browser.createUserContext", new WDEmptyParameters());
        }
    }

    public static class GetClientWindows extends WDCommandImpl<WDEmptyParameters> implements WDCommandData {
        public GetClientWindows() {
            super("browser.getClientWindows", new WDEmptyParameters());
        }
    }

    public static class GetUserContexts extends WDCommandImpl<WDEmptyParameters> implements WDCommandData {
        public GetUserContexts() {
            super("browser.getUserContexts", new WDEmptyParameters());
        }
    }

    public static class RemoveUserContext extends WDCommandImpl<RemoveUserContextParameters> implements WDCommandData {
        public RemoveUserContext(String contextId) {
            super("browser.removeUserContext", new RemoveUserContextParameters(new WDUserContext(contextId)));
        }

        public RemoveUserContext(WDUserContext context) {
            super("browser.removeUserContext", new RemoveUserContextParameters(context));
        }
    }

    public static class SetClientWindowState extends WDCommandImpl<SetClientWindowStateParameters> implements WDCommandData {
        public SetClientWindowState(String clientWindowId, String state) {
            super("browser.setClientWindowState",
                    new SetClientWindowStateParameters.ClientWindowNamedState( new WDClientWindow(clientWindowId),
                            SetClientWindowStateParameters.ClientWindowNamedState.State.valueOf(state)));
        }
        public SetClientWindowState(WDClientWindow WDClientWindow, SetClientWindowStateParameters.ClientWindowNamedState.State state) {
            super("browser.setClientWindowState",
                    new SetClientWindowStateParameters.ClientWindowNamedState(WDClientWindow, state));
        }
        // ToDo: These are not quite correct, since ClientWindow ought to be a separate parameter, not a part of the state:
        public SetClientWindowState(SetClientWindowStateParameters.ClientWindowNamedState clientWindowNamedState) {
            super("browser.setClientWindowState", clientWindowNamedState);
        }
        public SetClientWindowState(SetClientWindowStateParameters.ClientWindowRectState clientWindowRectState) {
            super("browser.setClientWindowState", clientWindowRectState);
        }
    }

}

