package de.bund.zrb.command.request.parameters.emulation;

import de.bund.zrb.type.browsingContext.WDBrowsingContext;
import de.bund.zrb.type.browser.WDUserContext;
import de.bund.zrb.type.emulation.WDForcedColorsModeTheme;

import java.util.List;

/**
 * Parameters for emulation.setForcedColorsModeThemeOverride
 */
public class SetForcedColorsModeThemeOverrideParameters {
    private WDForcedColorsModeTheme theme; // nullable
    private List<WDBrowsingContext> contexts; // optional
    private List<WDUserContext> userContexts; // optional

    public SetForcedColorsModeThemeOverrideParameters(WDForcedColorsModeTheme theme) {
        this.theme = theme;
    }

    public SetForcedColorsModeThemeOverrideParameters(WDForcedColorsModeTheme theme, List<WDBrowsingContext> contexts) {
        this.theme = theme;
        this.contexts = contexts;
    }

    public SetForcedColorsModeThemeOverrideParameters(List<WDUserContext> userContexts, WDForcedColorsModeTheme theme) {
        this.theme = theme;
        this.userContexts = userContexts;
    }

    public WDForcedColorsModeTheme getTheme() { return theme; }
    public List<WDBrowsingContext> getContexts() { return contexts; }
    public List<WDUserContext> getUserContexts() { return userContexts; }
}
