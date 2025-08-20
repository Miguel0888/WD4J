package de.bund.zrb.command.request.parameters.emulation;

import de.bund.zrb.type.browsingContext.WDBrowsingContext;
import de.bund.zrb.type.browser.WDUserContext;
import java.util.List;

/**
 * Parameters for emulation.setLocaleOverride
 */
public class SetLocaleOverrideParameters {
    private String locale; // nullable
    private List<WDBrowsingContext> contexts; // optional
    private List<WDUserContext> userContexts; // optional

    public SetLocaleOverrideParameters(String locale) {
        this.locale = locale;
    }

    public SetLocaleOverrideParameters(String locale, List<WDBrowsingContext> contexts) {
        this.locale = locale;
        this.contexts = contexts;
    }

    public SetLocaleOverrideParameters(List<WDUserContext> userContexts, String locale) {
        this.locale = locale;
        this.userContexts = userContexts;
    }

    public String getLocale() { return locale; }
    public List<WDBrowsingContext> getContexts() { return contexts; }
    public List<WDUserContext> getUserContexts() { return userContexts; }
}
