package de.bund.zrb.command.request;

import de.bund.zrb.api.markerInterfaces.WDCommandData;
import de.bund.zrb.command.request.helper.WDCommandImpl;
import de.bund.zrb.command.request.parameters.webExtension.ExtensionData;
import de.bund.zrb.command.request.parameters.webExtension.InstallParameters;
import de.bund.zrb.command.request.parameters.webExtension.UninstallParameters;
import de.bund.zrb.type.webExtension.WDExtension;

public class WDWebExtensionRequest {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Commands (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class Install extends WDCommandImpl<InstallParameters> implements WDCommandData {
        public Install(ExtensionData extensionData) {
            super("webExtension.install", new InstallParameters(extensionData));
        }
    }

    public static class Uninstall extends WDCommandImpl<UninstallParameters> implements WDCommandData {
        public Uninstall(WDExtension WDExtension) {
            super("webExtension.uninstall", new UninstallParameters(WDExtension));
        }
    }

}