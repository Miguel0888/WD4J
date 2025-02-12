package wd4j.impl.webdriver.command.request;

import wd4j.impl.markerInterfaces.CommandData;
import wd4j.impl.webdriver.command.request.helper.CommandImpl;
import wd4j.impl.webdriver.command.request.parameters.webExtension.ExtensionData;
import wd4j.impl.webdriver.command.request.parameters.webExtension.InstallParameters;
import wd4j.impl.webdriver.command.request.parameters.webExtension.UninstallParameters;
import wd4j.impl.webdriver.type.webExtension.Extension;
import wd4j.impl.websocket.Command;

public class WebExtensionRequest {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Commands (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class Install extends CommandImpl<InstallParameters> implements CommandData {
        public Install(ExtensionData extensionData) {
            super("webExtension.install", new InstallParameters(extensionData));
        }
    }

    public static class Uninstall extends CommandImpl<UninstallParameters> implements CommandData {
        public Uninstall(Extension extension) {
            super("webExtension.uninstall", new UninstallParameters(extension));
        }
    }

}