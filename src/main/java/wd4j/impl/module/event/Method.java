package wd4j.impl.module.event;

public class Method {
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Event methods
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    public static class session {}

    public static class browser {}

    public static class browsingContext
    {
        static String contextCreated = "browsingContext.contextCreated";
        static String contextDestroyed = "browsingContext.contextDestroyed";
        static String navigationStarted = "browsingContext.navigationStarted";
        static String fragmentNavigated = "browsingContext.fragmentNavigated";
        static String historyUpdated = "browsingContext.historyUpdated";
        static String domContentLoaded = "browsingContext.domContentLoaded";
        static String load = "browsingContext.load";
        static String downloadWillBegin = "browsingContext.downloadWillBegin";
        static String navigationAborted = "browsingContext.navigationAborted";
        static String navigationCommitted = "browsingContext.navigationCommitted";
        static String navigationFailed = "browsingContext.navigationFailed";
        static String userPromptClosed = "browsingContext.userPromptClosed";
        static String userPromptOpened = "browsingContext.userPromptOpened";
    }

    public static class network
    {
        static String authRequired = "network.authRequired";
        static String beforeRequestSent = "network.beforeRequestSent";
        static String fetchError = "network.fetchError";
        static String responseCompleted = "network.responseCompleted";
        static String responseStarted = "network.responseStarted";
    }

    public static class script
    {
        static String message = "script.message";
        static String realmCreated = "script.realmCreated";
        static String realmDestroyed = "script.realmDestroyed";
    }

    public static class storage {}

    public static class log
    {
        static String entryAdded = "log.entryAdded";
    }

    public static class input {}

    public static class webExtension {}

}
