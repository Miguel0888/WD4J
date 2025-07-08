package de.bund.zrb.websocket;

public enum WDEventNames {
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Event methods see: https://w3c.github.io/webdriver-bidi#modules
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // 🔹 Browsing Context Events
    CONTEXT_CREATED("browsingContext.contextCreated"),
    CONTEXT_DESTROYED("browsingContext.contextDestroyed"),
    NAVIGATION_STARTED("browsingContext.navigationStarted"),
    FRAGMENT_NAVIGATED("browsingContext.fragmentNavigated"),
    HISTORY_UPDATED("browsingContext.historyUpdated"),
    DOM_CONTENT_LOADED("browsingContext.domContentLoaded"),
    LOAD("browsingContext.load"),
    DOWNLOAD_WILL_BEGIN("browsingContext.downloadWillBegin"),
    NAVIGATION_ABORTED("browsingContext.navigationAborted"),
    NAVIGATION_COMMITTED("browsingContext.navigationCommitted"),
    NAVIGATION_FAILED("browsingContext.navigationFailed"),
    USER_PROMPT_CLOSED("browsingContext.userPromptClosed"),
    USER_PROMPT_OPENED("browsingContext.userPromptOpened"),

    // 🔹 Network Events
    AUTH_REQUIRED("network.authRequired"),
    BEFORE_REQUEST_SENT("network.beforeRequestSent"),
    FETCH_ERROR("network.fetchError"),
    RESPONSE_COMPLETED("network.responseCompleted"),
    RESPONSE_STARTED("network.responseStarted"),

    // 🔹 Script Events
    MESSAGE("script.message"),
    REALM_CREATED("script.realmCreated"),
    REALM_DESTROYED("script.realmDestroyed"),

    // 🔹 Log Events
    ENTRY_ADDED("log.entryAdded"),

    // 🔹 Input Events
    FILE_DIALOG_OPENED("input.fileDialogOpened");


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
