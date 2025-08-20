package de.bund.zrb.command.request.parameters.emulation;

import de.bund.zrb.api.WDCommand;
import de.bund.zrb.type.browsingContext.WDBrowsingContext;
import de.bund.zrb.type.browser.WDUserContext;
import java.util.List;

/**
 * Parameters for emulation.setTimezoneOverride.
 */
public class SetTimezoneOverrideParameters implements WDCommand.Params {
    private String timezone; // nullable
    private List<WDBrowsingContext> contexts; // optional
    private List<WDUserContext> userContexts; // optional

    public SetTimezoneOverrideParameters(String timezone) {
        this.timezone = timezone;
    }

    public SetTimezoneOverrideParameters(String timezone, List<WDBrowsingContext> contexts) {
        this.timezone = timezone;
        this.contexts = contexts;
    }

    public SetTimezoneOverrideParameters(List<WDUserContext> userContexts, String timezone) {
        this.timezone = timezone;
        this.userContexts = userContexts;
    }

    public String getTimezone() { return timezone; }
    public List<WDBrowsingContext> getContexts() { return contexts; }
    public List<WDUserContext> getUserContexts() { return userContexts; }
}