package wd4j.impl.manager;

import wd4j.impl.markerInterfaces.WDModule;
import wd4j.impl.webdriver.command.request.WDWebExtensionRequest;
import wd4j.impl.webdriver.command.request.parameters.webExtension.ExtensionData;
import wd4j.impl.webdriver.command.response.WDWebExtensionResult;
import wd4j.impl.webdriver.type.webExtension.WDExtension;
import wd4j.impl.websocket.WebSocketManager;

import wd4j.impl.webdriver.command.response.WDEmptyResult;

public class WDWebExtensionManager implements WDModule {

    public WDExtension WDExtension;

    private final WebSocketManager webSocketManager;

    public WDWebExtensionManager(WebSocketManager webSocketManager) {
        this.webSocketManager = webSocketManager;
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
            WDWebExtensionResult.InstallResult result = webSocketManager.sendAndWaitForResponse(
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
            webSocketManager.sendAndWaitForResponse(
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