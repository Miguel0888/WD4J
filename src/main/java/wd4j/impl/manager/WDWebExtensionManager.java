package wd4j.impl.manager;

import wd4j.impl.markerInterfaces.WDModule;
import wd4j.impl.webdriver.command.request.WebExtensionRequest;
import wd4j.impl.webdriver.command.request.parameters.webExtension.ExtensionData;
import wd4j.impl.webdriver.type.webExtension.WDExtension;
import wd4j.impl.websocket.WebSocketManager;

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
     *
     * @throws RuntimeException if the installation fails.
     */
    public void install(ExtensionData extensionData) {
        try {
            webSocketManager.sendAndWaitForResponse(new WebExtensionRequest.Install(extensionData), String.class);
            System.out.println("Web extension installed of Type " + extensionData.getType());
        } catch (RuntimeException e) {
            System.out.println("Error installing web extension: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Uninstalls a web extension from the specified browsing context.
     *
     * @param WDExtension The ID of the extension to uninstall.
     * @throws RuntimeException if the uninstallation fails.
     */
    public void uninstall(WDExtension WDExtension) {
        try {
            webSocketManager.sendAndWaitForResponse(new WebExtensionRequest.Uninstall(WDExtension), String.class);
            System.out.println("Web extension uninstalled: " + WDExtension.value());
        } catch (RuntimeException e) {
            System.out.println("Error uninstalling web extension: " + e.getMessage());
            throw e;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Events (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}