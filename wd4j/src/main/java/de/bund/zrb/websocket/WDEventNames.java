package de.bund.zrb.websocket;

public enum WDEventNames {
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Event methods see: https://w3c.github.io/webdriver-bidi#modules
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // -> marks the Playwright mapping, otherwise see 'BiDiExtraHookInstaller' Class
    // 🔹 Browsing Context Events
    CONTEXT_CREATED("browsingContext.contextCreated"), // -> onWebSocket, onFrameAttached, onPopup
    CONTEXT_DESTROYED("browsingContext.contextDestroyed"), // -> onClose
    NAVIGATION_STARTED("browsingContext.navigationStarted"), // -> onFrameNavigated
    FRAGMENT_NAVIGATED("browsingContext.fragmentNavigated"),
    HISTORY_UPDATED("browsingContext.historyUpdated"),
    DOM_CONTENT_LOADED("browsingContext.domContentLoaded"), // -> onDOMContentLoaded
    LOAD("browsingContext.load"), // -> onLoad
    DOWNLOAD_WILL_BEGIN("browsingContext.downloadWillBegin"), // -> onDownload
    NAVIGATION_ABORTED("browsingContext.navigationAborted"),
    NAVIGATION_COMMITTED("browsingContext.navigationCommitted"),
    NAVIGATION_FAILED("browsingContext.navigationFailed"), // -> onCrash
    USER_PROMPT_CLOSED("browsingContext.userPromptClosed"),
    USER_PROMPT_OPENED("browsingContext.userPromptOpened"), // -> onDialog

    // 🔹 Network Events
    AUTH_REQUIRED("network.authRequired"),
    BEFORE_REQUEST_SENT("network.beforeRequestSent"), // -> onRequest
    FETCH_ERROR("network.fetchError"), // -> onRequestFailed
    RESPONSE_COMPLETED("network.responseCompleted"), // -> onRequestFinished
    RESPONSE_STARTED("network.responseStarted"), // -> onResponse

    // 🔹 Script Events
    MESSAGE("script.message"), // -> non PlayWright official: BrowserImpl#onMessage
    REALM_CREATED("script.realmCreated"), // -> onWorker
    REALM_DESTROYED("script.realmDestroyed"),

    // 🔹 Log Events
    ENTRY_ADDED("log.entryAdded"), // -> onConsoleMessage

    // 🔹 Input Events
    FILE_DIALOG_OPENED("input.fileDialogOpened"); // -> onFileChooser


    // 🔹 Weitere Module (Session, Browser Storage, Input, WebExtension) haben aktuell laut W3C-Spec keine Events

    private final String name;

    WDEventNames(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    // 🔹 Methode zur Suche eines Events anhand des Namens (für Dispatcher)
    public static WDEventNames fromName(String name) {
        for (WDEventNames event : WDEventNames.values()) {
            if (event.getName().equals(name)) {
                return event;
            }
        }
        return null; // Falls kein passendes Event gefunden wird
    }
}
