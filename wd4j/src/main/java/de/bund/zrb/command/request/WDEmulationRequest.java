package de.bund.zrb.command.request;

import de.bund.zrb.api.markerInterfaces.WDCommandData;
import de.bund.zrb.command.request.helper.WDCommandImpl;
import de.bund.zrb.command.request.parameters.emulation.*;

/**
 * Provides wrapper classes for WebDriver BiDi emulation.* commands.
 * Each inner class corresponds to a specific command defined in the specification.
 */
public class WDEmulationRequest {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Commands (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /** Wrapper for emulation.setForcedColorsModeThemeOverride */
    public static class SetForcedColorsModeThemeOverride extends WDCommandImpl<SetForcedColorsModeThemeOverrideParameters> implements WDCommandData {
        public SetForcedColorsModeThemeOverride(SetForcedColorsModeThemeOverrideParameters params) {
            super("emulation.setForcedColorsModeThemeOverride", params);
        }
    }

    /** Wrapper for emulation.setGeolocationOverride */
    public static class SetGeolocationOverride extends WDCommandImpl<SetGeolocationOverrideParameters> implements WDCommandData {
        public SetGeolocationOverride(SetGeolocationOverrideParameters params) {
            super("emulation.setGeolocationOverride", params);
        }
    }

    /** Wrapper for emulation.setLocaleOverride */
    public static class SetLocaleOverride extends WDCommandImpl<SetLocaleOverrideParameters> implements WDCommandData {
        public SetLocaleOverride(SetLocaleOverrideParameters params) {
            super("emulation.setLocaleOverride", params);
        }
    }

    /** Wrapper for emulation.setScreenOrientationOverride */
    public static class SetScreenOrientationOverride extends WDCommandImpl<SetScreenOrientationOverrideParameters> implements WDCommandData {
        public SetScreenOrientationOverride(SetScreenOrientationOverrideParameters params) {
            super("emulation.setScreenOrientationOverride", params);
        }
    }

    /** Wrapper for emulation.setScriptingEnabled */
    public static class SetScriptingEnabled extends WDCommandImpl<SetScriptingEnabledParameters> implements WDCommandData {
        public SetScriptingEnabled(SetScriptingEnabledParameters params) {
            super("emulation.setScriptingEnabled", params);
        }
    }

    /** Wrapper for emulation.setTimezoneOverride */
    public static class SetTimezoneOverride extends WDCommandImpl<SetTimezoneOverrideParameters> implements WDCommandData {
        public SetTimezoneOverride(SetTimezoneOverrideParameters params) {
            super("emulation.setTimezoneOverride", params);
        }
    }
}