package wd4j.impl.dto.command.request;

import wd4j.impl.markerInterfaces.WDCommandData;
import wd4j.impl.dto.command.request.helper.WDCommandImpl;
import wd4j.impl.dto.command.request.parameters.webExtension.ExtensionData;
import wd4j.impl.dto.command.request.parameters.webExtension.InstallParameters;
import wd4j.impl.dto.command.request.parameters.webExtension.UninstallParameters;
import wd4j.impl.dto.type.webExtension.WDExtension;

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