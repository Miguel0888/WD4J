package de.bund.zrb.command.request;

import de.bund.zrb.api.markerInterfaces.WDCommandData;
import de.bund.zrb.command.request.helper.WDCommandImpl;
import de.bund.zrb.command.request.parameters.emulation.*;

public class WDEmulationRequest {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Commands (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class SetForcedColorsModeThemeOverride extends WDCommandImpl<SetForcedColorsModeThemeOverrideParameters> implements WDCommandData {
        public SetForcedColorsModeThemeOverride(SetForcedColorsModeThemeOverrideParameters params) {
            super("emulation.setForcedColorsModeThemeOverride", params);
        }
    }

    public static class SetGeolocationOverride extends WDCommandImpl<SetGeolocationOverrideParameters> implements WDCommandData {
        public SetGeolocationOverride(SetGeolocationOverrideParameters params) {
            super("emulation.setGeolocationOverride", params);
        }
    }

    public static class SetLocaleOverride extends WDCommandImpl<SetLocaleOverrideParameters> implements WDCommandData {
        public SetLocaleOverride(SetLocaleOverrideParameters params) {
            super("emulation.setLocaleOverride", params);
        }
    }

    public static class SetScreenOrientationOverride extends WDCommandImpl<SetScreenOrientationOverrideParameters> implements WDCommandData {
        public SetScreenOrientationOverride(SetScreenOrientationOverrideParameters params) {
            super("emulation.setScreenOrientationOverride", params);
        }
    }

    public static class SetScriptingEnabled extends WDCommandImpl<SetScriptingEnabledParameters> implements WDCommandData {
        public SetScriptingEnabled(SetScriptingEnabledParameters params) {
            super("emulation.setScriptingEnabled", params);
        }
    }

    public static class SetTimezoneOverride extends WDCommandImpl<SetTimezoneOverrideParameters> implements WDCommandData {
        public SetTimezoneOverride(SetTimezoneOverrideParameters params) {
            super("emulation.setTimezoneOverride", params);
        }
    }
}
