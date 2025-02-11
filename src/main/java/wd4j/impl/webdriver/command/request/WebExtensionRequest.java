package wd4j.impl.webdriver.command.request;

import wd4j.impl.markerInterfaces.CommandData;
import wd4j.impl.webdriver.command.request.helper.CommandImpl;
import wd4j.impl.webdriver.command.request.parameters.webExtension.InstallParameters;
import wd4j.impl.webdriver.command.request.parameters.webExtension.UninstallParameters;
import wd4j.impl.websocket.Command;

public class WebExtensionRequest {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Commands (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class Install extends CommandImpl<InstallParameters> implements CommandData {
        public Install(String contextId, String extensionPath) {
            super("webExtension.install", new InstallParameters(contextId, extensionPath));
        }
    }

    public static class Uninstall extends CommandImpl<UninstallParameters> implements CommandData {
        public Uninstall(String contextId, String extensionId) {
            super("webExtension.uninstall", new UninstallParameters(contextId, extensionId));
        }
    }

}