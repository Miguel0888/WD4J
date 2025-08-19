package de.bund.zrb.manager;

import de.bund.zrb.api.markerInterfaces.WDModule;
import de.bund.zrb.command.request.WDWebExtensionRequest;
import de.bund.zrb.command.request.parameters.webExtension.ExtensionData;
import de.bund.zrb.command.response.WDWebExtensionResult;
import de.bund.zrb.type.webExtension.WDExtension;
import de.bund.zrb.api.WDWebSocketManager;

import de.bund.zrb.command.response.WDEmptyResult;

public class WDWebExtensionManager implements WDModule {

    private final WDWebSocketManager WDWebSocketManager;

    public WDWebExtensionManager(WDWebSocketManager WDWebSocketManager) {
        this.WDWebSocketManager = WDWebSocketManager;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Event Handlers
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Commands
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Installs a web extension in the specified browsing context.
     *
     * @param extensionData The extension of a specific type to install. See {@link ExtensionData} for more information.
     * @return The installed extension result.
     * @throws RuntimeException if the installation fails.
     */
    public WDWebExtensionResult.InstallResult install(ExtensionData extensionData) {
        try {
            WDWebExtensionResult.InstallResult result = WDWebSocketManager.sendAndWaitForResponse(
                    new WDWebExtensionRequest.Install(extensionData), WDWebExtensionResult.InstallResult.class
            );
            System.out.println("Web extension installed: " + result.getExtension().value());
            return result;
        } catch (RuntimeException e) {
            System.out.println("Error installing web extension: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Uninstalls a web extension from the specified browsing context.
     *
     * @param extension The extension to uninstall.
     * @throws RuntimeException if the uninstallation fails.
     */
    public void uninstall(WDExtension extension) {
        try {
            WDWebSocketManager.sendAndWaitForResponse(
                    new WDWebExtensionRequest.Uninstall(extension), WDEmptyResult.class
            );
            System.out.println("Web extension uninstalled: " + extension.value());
        } catch (RuntimeException e) {
            System.out.println("Error uninstalling web extension: " + e.getMessage());
            throw e;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Events (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}