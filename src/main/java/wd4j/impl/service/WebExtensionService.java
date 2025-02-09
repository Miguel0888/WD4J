package wd4j.impl.service;

import wd4j.impl.markerInterfaces.Module;
import wd4j.impl.webdriver.command.request.WebExtensionRequest;
import wd4j.impl.webdriver.type.webExtension.Extension;
import wd4j.impl.playwright.WebSocketImpl;

public class WebExtensionService implements Module {

    public Extension extension;

    private final WebSocketImpl webSocketImpl;

    public WebExtensionService(WebSocketImpl webSocketImpl) {
        this.webSocketImpl = webSocketImpl;
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
     * @param contextId     The ID of the browsing context.
     * @param extensionPath The path to the extension to install.
     * @throws RuntimeException if the installation fails.
     */
    public void install(String contextId, String extensionPath) {
        try {
            webSocketImpl.sendAndWaitForResponse(new WebExtensionRequest.Install(contextId, extensionPath));
            System.out.println("Web extension installed from path: " + extensionPath + " in context: " + contextId);
        } catch (RuntimeException e) {
            System.out.println("Error installing web extension: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Uninstalls a web extension from the specified browsing context.
     *
     * @param contextId   The ID of the browsing context.
     * @param extensionId The ID of the extension to uninstall.
     * @throws RuntimeException if the uninstallation fails.
     */
    public void uninstall(String contextId, String extensionId) {
        try {
            webSocketImpl.sendAndWaitForResponse(new WebExtensionRequest.Uninstall(contextId, extensionId));
            System.out.println("Web extension uninstalled: " + extensionId + " from context: " + contextId);
        } catch (RuntimeException e) {
            System.out.println("Error uninstalling web extension: " + e.getMessage());
            throw e;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Events (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}