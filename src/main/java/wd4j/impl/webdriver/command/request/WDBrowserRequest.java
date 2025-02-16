package wd4j.impl.webdriver.command.request;

import wd4j.impl.markerInterfaces.WDCommandData;
import wd4j.impl.webdriver.command.request.helper.WDCommandImpl;
import wd4j.impl.webdriver.command.request.helper.WDEmptyParameters;
import wd4j.impl.webdriver.command.request.parameters.browser.WDRemoveUserContextParameters;
import wd4j.impl.webdriver.command.request.parameters.browser.WDSetClientWindowStateParameters;
import wd4j.impl.webdriver.type.browser.WDClientWindow;
import wd4j.impl.webdriver.type.browser.WDUserContext;

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

    public static class RemoveUserContext extends WDCommandImpl<WDRemoveUserContextParameters> implements WDCommandData {
        public RemoveUserContext(String contextId) {
            super("browser.removeUserContext", new WDRemoveUserContextParameters(new WDUserContext(contextId)));
        }

        public RemoveUserContext(WDUserContext context) {
            super("browser.removeUserContext", new WDRemoveUserContextParameters(context));
        }
    }

    public static class SetClientWindowState extends WDCommandImpl<WDSetClientWindowStateParameters> implements WDCommandData {
        public SetClientWindowState(String clientWindowId, String state) {
            super("browser.setClientWindowState",
                    new WDSetClientWindowStateParameters.ClientWindowNamedStateWD( new WDClientWindow(clientWindowId),
                            WDSetClientWindowStateParameters.ClientWindowNamedStateWD.State.valueOf(state)));
        }
        public SetClientWindowState(WDClientWindow WDClientWindow, WDSetClientWindowStateParameters.ClientWindowNamedStateWD.State state) {
            super("browser.setClientWindowState",
                    new WDSetClientWindowStateParameters.ClientWindowNamedStateWD(WDClientWindow, state));
        }
        // ToDo: These are not quite correct, since ClientWindow ought to be a separate parameter, not a part of the state:
        public SetClientWindowState(WDSetClientWindowStateParameters.ClientWindowNamedStateWD clientWindowNamedState) {
            super("browser.setClientWindowState", clientWindowNamedState);
        }
        public SetClientWindowState(WDSetClientWindowStateParameters.ClientWindowRectStateWD clientWindowRectState) {
            super("browser.setClientWindowState", clientWindowRectState);
        }
    }

}

