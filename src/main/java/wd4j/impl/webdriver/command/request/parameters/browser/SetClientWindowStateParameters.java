package wd4j.impl.webdriver.command.request.parameters.browser;

import wd4j.impl.webdriver.type.browser.ClientWindow;
import wd4j.impl.websocket.Command;

public class SetClientWindowStateParameters implements Command.Params {
    private final ClientWindow clientWindow;
    private final String windowState;

    public SetClientWindowStateParameters(ClientWindow clientWindow, String windowState) {
        this.clientWindow = clientWindow;
        this.windowState = windowState;
    }

    public ClientWindow getClientWindow() {
        return clientWindow;
    }

    public String getWindowState() {
        return windowState;
    }
}
