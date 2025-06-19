package de.bund.zrb.command.request.parameters.session.parameters;

import de.bund.zrb.type.session.WDCapabilitiesRequest;
import de.bund.zrb.api.WDCommand;

/**
 * The session.new command allows creating a new BiDi session.
 * A BiDi session is a session which has the BiDi flag set to true.
 *
 * WebDriver BiDi extends the session concept from WebDriver.
 * A WebDriver session in general has a BiDi flag, which is false unless otherwise stated.
 */
public class NewParameters implements WDCommand.Params {
    private final WDCapabilitiesRequest capabilities;

    public NewParameters(WDCapabilitiesRequest capabilities) {
        this.capabilities = capabilities;
    }

    public WDCapabilitiesRequest getCapabilities() {
        return capabilities;
    }
}
