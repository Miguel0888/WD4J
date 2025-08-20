package de.bund.zrb.command.request.parameters.emulation;

import de.bund.zrb.api.WDCommand;
import de.bund.zrb.type.browsingContext.WDBrowsingContext;
import de.bund.zrb.type.browser.WDUserContext;
import java.util.List;

/**
 * Parameters for emulation.setScriptingEnabled.
 */
public class SetScriptingEnabledParameters implements WDCommand.Params {
    private Boolean enabled; // false | null -> reset when null
    private List<WDBrowsingContext> contexts; // optional
    private List<WDUserContext> userContexts; // optional

    public SetScriptingEnabledParameters(Boolean enabled) {
        this.enabled = enabled;
    }

    public SetScriptingEnabledParameters(Boolean enabled, List<WDBrowsingContext> contexts) {
        this.enabled = enabled;
        this.contexts = contexts;
    }

    public SetScriptingEnabledParameters(List<WDUserContext> userContexts, Boolean enabled) {
        this.enabled = enabled;
        this.userContexts = userContexts;
    }

    public Boolean getEnabled() { return enabled; }
    public List<WDBrowsingContext> getContexts() { return contexts; }
    public List<WDUserContext> getUserContexts() { return userContexts; }
}