package wd4j.impl.manager;

import wd4j.impl.markerInterfaces.Module;
import wd4j.impl.webdriver.command.request.WebExtensionRequest;
import wd4j.impl.webdriver.command.request.parameters.webExtension.ExtensionData;
import wd4j.impl.webdriver.type.webExtension.Extension;
import wd4j.impl.playwright.WebSocketImpl;
import wd4j.impl.websocket.CommunicationManager;

public class WebExtensionManager implements Module {

    public Extension extension;

    private final CommunicationManager communicationManager;

    public WebExtensionManager(CommunicationManager communicationManager) {
        this.communicationManager = communicationManager;
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
            communicationManager.getWebSocket().sendAndWaitForResponse(new WebExtensionRequest.Install(extensionData));
            System.out.println("Web extension installed of Type " + extensionData.getType());
        } catch (RuntimeException e) {
            System.out.println("Error installing web extension: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Uninstalls a web extension from the specified browsing context.
     *
     * @param extension The ID of the extension to uninstall.
     * @throws RuntimeException if the uninstallation fails.
     */
    public void uninstall(Extension extension) {
        try {
            communicationManager.getWebSocket().sendAndWaitForResponse(new WebExtensionRequest.Uninstall(extension));
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