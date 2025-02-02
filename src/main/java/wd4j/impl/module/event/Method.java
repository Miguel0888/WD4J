package wd4j.impl.module.event;

import wd4j.api.*;

public enum Method {
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Event methods see: https://w3c.github.io/webdriver-bidi#modules
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // 🔹 SESSION (noch nicht relevant)
    SESSION("session", null),

    // 🔹 BROWSER (noch nicht relevant)
    BROWSER("browser", null),

    // 🔹 Browsing Context Events
    CONTEXT_CREATED("browsingContext.contextCreated", null),
    CONTEXT_DESTROYED("browsingContext.contextDestroyed", null),
    NAVIGATION_STARTED("browsingContext.navigationStarted", null),
    FRAGMENT_NAVIGATED("browsingContext.fragmentNavigated", null),
    HISTORY_UPDATED("browsingContext.historyUpdated", null),
    DOM_CONTENT_LOADED("browsingContext.domContentLoaded", Page.class),
    LOAD("browsingContext.load", Page.class),
    DOWNLOAD_WILL_BEGIN("browsingContext.downloadWillBegin", null),
    NAVIGATION_ABORTED("browsingContext.navigationAborted", null),
    NAVIGATION_COMMITTED("browsingContext.navigationCommitted", null),
    NAVIGATION_FAILED("browsingContext.navigationFailed", null),
    USER_PROMPT_CLOSED("browsingContext.userPromptClosed", null),
    USER_PROMPT_OPENED("browsingContext.userPromptOpened", null),

    // 🔹 Network Events
    AUTH_REQUIRED("network.authRequired", null),
    BEFORE_REQUEST_SENT("network.beforeRequestSent", Request.class),
    FETCH_ERROR("network.fetchError", null),
    RESPONSE_COMPLETED("network.responseCompleted", Response.class),
    RESPONSE_STARTED("network.responseStarted", Response.class),

    // 🔹 Script Events
    MESSAGE("script.message", null),
    REALM_CREATED("script.realmCreated", null),
    REALM_DESTROYED("script.realmDestroyed", null),

    // 🔹 Log Events
    ENTRY_ADDED("log.entryAdded", ConsoleMessage.class);

    // 🔹 Weitere Module (Storage, Input, WebExtension) können später ergänzt werden

    private final String name;
    private final Class<?> associatedClass;

    Method(String name, Class<?> associatedClass) {
        this.name = name;
        this.associatedClass = associatedClass;
    }

    public String getName() {
        return name;
    }

    public Class<?> getAssociatedClass() {
        return associatedClass;
    }

    // 🔹 Methode zur Suche eines Events anhand des Namens (für Dispatcher)
    public static Method fromName(String name) {
        for (Method event : Method.values()) {
            if (event.getName().equals(name)) {
                return event;
            }
        }
        return null; // Falls kein passendes Event gefunden wird
    }
}
