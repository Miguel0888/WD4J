package wd4j.impl.module.event;

public class Method {
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Event methods see: https://w3c.github.io/webdriver-bidi#modules
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class session {}

    public static class browser {}

    public static class browsingContext
    {
        static final String contextCreated = "browsingContext.contextCreated";
        static final String contextDestroyed = "browsingContext.contextDestroyed";
        static final String navigationStarted = "browsingContext.navigationStarted";
        static final String fragmentNavigated = "browsingContext.fragmentNavigated";
        static final String historyUpdated = "browsingContext.historyUpdated";
        static final String domContentLoaded = "browsingContext.domContentLoaded";
        static final String load = "browsingContext.load";
        static final String downloadWillBegin = "browsingContext.downloadWillBegin";
        static final String navigationAborted = "browsingContext.navigationAborted";
        static final String navigationCommitted = "browsingContext.navigationCommitted";
        static final String navigationFailed = "browsingContext.navigationFailed";
        static final String userPromptClosed = "browsingContext.userPromptClosed";
        static final String userPromptOpened = "browsingContext.userPromptOpened";
    }

    public static class network
    {
        static final String authRequired = "network.authRequired";
        static final String beforeRequestSent = "network.beforeRequestSent";
        static final String fetchError = "network.fetchError";
        static final String responseCompleted = "network.responseCompleted";
        static final String responseStarted = "network.responseStarted";
    }

    public static class script
    {
        static final String message = "script.message";
        static final String realmCreated = "script.realmCreated";
        static final String realmDestroyed = "script.realmDestroyed";
    }

    public static class storage {}

    public static class log
    {
        static final String entryAdded = "log.entryAdded";
    }

    public static class input {}

    public static class webExtension {}

}
