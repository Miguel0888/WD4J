package de.bund.zrb.command.request.parameters.emulation;

import de.bund.zrb.api.WDCommand;
import de.bund.zrb.type.browsingContext.WDBrowsingContext;
import de.bund.zrb.type.browser.WDUserContext;
import de.bund.zrb.type.emulation.WDScreenOrientation;
import java.util.List;

/**
 * Parameters for emulation.setScreenOrientationOverride.
 */
public class SetScreenOrientationOverrideParameters implements WDCommand.Params {
    private WDScreenOrientation screenOrientation; // nullable
    private List<WDBrowsingContext> contexts; // optional
    private List<WDUserContext> userContexts; // optional

    public SetScreenOrientationOverrideParameters(WDScreenOrientation screenOrientation) {
        this.screenOrientation = screenOrientation;
    }

    public SetScreenOrientationOverrideParameters(WDScreenOrientation screenOrientation, List<WDBrowsingContext> contexts) {
        this.screenOrientation = screenOrientation;
        this.contexts = contexts;
    }

    public SetScreenOrientationOverrideParameters(List<WDUserContext> userContexts, WDScreenOrientation screenOrientation) {
        this.screenOrientation = screenOrientation;
        this.userContexts = userContexts;
    }

    public WDScreenOrientation getScreenOrientation() { return screenOrientation; }
    public List<WDBrowsingContext> getContexts() { return contexts; }
    public List<WDUserContext> getUserContexts() { return userContexts; }
}