package wd4j.impl.webdriver.command.request.parameters.session;

import wd4j.impl.webdriver.type.session.CapabilitiesRequest;
import wd4j.impl.websocket.Command;

public class NewParameters implements Command.Params {
    private final CapabilitiesRequest capabilities;

    public NewParameters(CapabilitiesRequest capabilities) {
        this.capabilities = capabilities;
    }

    public CapabilitiesRequest getCapabilities() {
        return capabilities;
    }
}
