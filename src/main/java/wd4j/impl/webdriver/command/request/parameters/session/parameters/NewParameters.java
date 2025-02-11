package wd4j.impl.webdriver.command.request.parameters.session.parameters;

import wd4j.impl.webdriver.type.session.CapabilitiesRequest;
import wd4j.impl.websocket.Command;

/**
 * The session.new command allows creating a new BiDi session.
 * A BiDi session is a session which has the BiDi flag set to true.
 *
 * WebDriver BiDi extends the session concept from WebDriver.
 * A WebDriver session in general has a BiDi flag, which is false unless otherwise stated.
 */
public class NewParameters implements Command.Params {
    private final CapabilitiesRequest capabilities;

    public NewParameters(CapabilitiesRequest capabilities) {
        this.capabilities = capabilities;
    }

    public CapabilitiesRequest getCapabilities() {
        return capabilities;
    }
}
