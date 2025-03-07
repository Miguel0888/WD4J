package wd4j.impl.webdriver.event;

import wd4j.api.*;

public enum WDEventMapping {
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Event methods see: https://w3c.github.io/webdriver-bidi#modules
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // 🔹 Browsing Context Events
    CONTEXT_CREATED("browsingContext.contextCreated", Page.class),
    CONTEXT_DESTROYED("browsingContext.contextDestroyed", Page.class),
    NAVIGATION_STARTED("browsingContext.navigationStarted", Frame.class),
    FRAGMENT_NAVIGATED("browsingContext.fragmentNavigated", Frame.class),
    HISTORY_UPDATED("browsingContext.historyUpdated", Frame.class),
    DOM_CONTENT_LOADED("browsingContext.domContentLoaded", Page.class),
    LOAD("browsingContext.load", Page.class),
    DOWNLOAD_WILL_BEGIN("browsingContext.downloadWillBegin", Download.class),
    NAVIGATION_ABORTED("browsingContext.navigationAborted", Frame.class),
    NAVIGATION_COMMITTED("browsingContext.navigationCommitted", Frame.class),
    NAVIGATION_FAILED("browsingContext.navigationFailed", Frame.class),
    USER_PROMPT_CLOSED("browsingContext.userPromptClosed", Dialog.class),
    USER_PROMPT_OPENED("browsingContext.userPromptOpened", Dialog.class),

    // 🔹 Network Events
    AUTH_REQUIRED("network.authRequired", Request.class),
    BEFORE_REQUEST_SENT("network.beforeRequestSent", Request.class),
    FETCH_ERROR("network.fetchError", Response.class),
    RESPONSE_COMPLETED("network.responseCompleted", Response.class),
    RESPONSE_STARTED("network.responseStarted", Response.class),

    // 🔹 Script Events
    MESSAGE("script.message", ConsoleMessage.class),
    REALM_CREATED("script.realmCreated", Worker.class),
    REALM_DESTROYED("script.realmDestroyed", Worker.class),

    // 🔹 Log Events
    ENTRY_ADDED("log.entryAdded", ConsoleMessage.class);

    // 🔹 Weitere Module (Session, Browser Storage, Input, WebExtension) haben aktuell laut W3C-Spec keine Events

    private final String name;
    private final Class<?> associatedClass;

    WDEventMapping(String name, Class<?> associatedClass) {
        this.name = name;
        this.associatedClass = associatedClass;
    }

    public String getName() {
        return name;
    }

    @Deprecated // since direct mapping to playwright is not working in all cases
    public Class<?> getAssociatedClass() {
        return associatedClass;
    }

    // 🔹 Methode zur Suche eines Events anhand des Namens (für Dispatcher)
    public static WDEventMapping fromName(String name) {
        for (WDEventMapping event : WDEventMapping.values()) {
            if (event.getName().equals(name)) {
                return event;
            }
        }
        return null; // Falls kein passendes Event gefunden wird
    }
}
