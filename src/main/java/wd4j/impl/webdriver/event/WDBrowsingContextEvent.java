package wd4j.impl.webdriver.event;

import com.google.gson.JsonObject;
import wd4j.impl.markerInterfaces.WDModule;
import wd4j.impl.webdriver.type.browsingContext.WDBrowsingContext;
import wd4j.impl.webdriver.type.browsingContext.WDInfo;
import wd4j.impl.webdriver.type.browsingContext.WDNavigationInfo;
import wd4j.impl.webdriver.type.browsingContext.WDUserPromptType;
import wd4j.impl.webdriver.type.session.WDUserPromptHandlerType;
import wd4j.impl.websocket.WDEvent;

public class WDBrowsingContextEvent implements WDModule {
    public WDBrowsingContextEvent(JsonObject json) {
        // TODO: Implement mapping from JSON to Java Object
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Event Types (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class Created extends WDEvent<WDInfo> {
        private String method = WDEventMapping.CONTEXT_CREATED.name();

        public Created(JsonObject json) {
            super(json, WDInfo.class);
        }

        @Override
        public String getMethod() {
            return method;
        }
    }


    public static class Destroyed extends WDEvent<WDInfo> {
        private String method = WDEventMapping.CONTEXT_DESTROYED.getName();

        public Destroyed(JsonObject json) {
            super(json, WDInfo.class);
        }

        @Override
        public String getMethod() {
            return method;
        }
    }

    public static class NavigationStarted extends WDEvent<WDNavigationInfo> {
        private String method = WDEventMapping.NAVIGATION_STARTED.getName();

        public NavigationStarted(JsonObject json) {
            super(json, WDNavigationInfo.class);
        }

        @Override
        public String getMethod() {
            return method;
        }
    }

    public static class FragmentNavigated extends WDEvent<WDNavigationInfo> {
        private String method = WDEventMapping.FRAGMENT_NAVIGATED.getName();

        public FragmentNavigated(JsonObject json) {
            super(json, WDNavigationInfo.class);
        }

        @Override
        public String getMethod() {
            return method;
        }
    }

    public static class DomContentLoaded extends WDEvent<WDNavigationInfo> {
        private String method = WDEventMapping.DOM_CONTENT_LOADED.getName();

        public DomContentLoaded(JsonObject json) {
            super(json, WDNavigationInfo.class);
        }

        @Override
        public String getMethod() {
            return method;
        }
    }

    public static class Load extends WDEvent<WDNavigationInfo> {
        private String method = WDEventMapping.LOAD.getName();

        public Load(JsonObject json) {
            super(json, WDNavigationInfo.class);
        }

        @Override
        public String getMethod() {
            return method;
        }
    }

    public static class DownloadWillBegin extends WDEvent<WDNavigationInfo> {
        private String method = WDEventMapping.DOWNLOAD_WILL_BEGIN.getName();

        public DownloadWillBegin(JsonObject json) {
            super(json, WDNavigationInfo.class);
        }

        @Override
        public String getMethod() {
            return method;
        }
    }

    public static class NavigationAborted extends WDEvent<WDNavigationInfo> {
        private String method = WDEventMapping.NAVIGATION_ABORTED.getName();

        public NavigationAborted(JsonObject json) {
            super(json, WDNavigationInfo.class);
        }

        @Override
        public String getMethod() {
            return method;
        }
    }

    public static class NavigationFailed extends WDEvent<WDNavigationInfo> {
        private String method = WDEventMapping.NAVIGATION_FAILED.getName();

        public NavigationFailed(JsonObject json) {
            super(json, WDNavigationInfo.class);
        }

        @Override
        public String getMethod() {
            return method;
        }
    }

    public static class UserPromptClosed extends WDEvent<UserPromptClosed.UserPromptClosedParameters> {
        private String method = WDEventMapping.USER_PROMPT_CLOSED.getName();

        public UserPromptClosed(JsonObject json) {
            super(json, UserPromptClosed.UserPromptClosedParameters.class);
        }

        @Override
        public String getMethod() {
            return method;
        }

        public static class UserPromptClosedParameters {
            private String context; // browsingContext.BrowsingContext (vermutlich eine ID als String)
            private boolean accepted;
            private WDUserPromptType type;
            private String userText; // Optional

            public UserPromptClosedParameters(String context, boolean accepted, WDUserPromptType type, String userText) {
                this.context = context;
                this.accepted = accepted;
                this.type = type;
                this.userText = userText;
            }

            public String getContext() {
                return context;
            }

            public void setContext(String context) {
                this.context = context;
            }

            public boolean isAccepted() {
                return accepted;
            }

            public void setAccepted(boolean accepted) {
                this.accepted = accepted;
            }

            public WDUserPromptType getType() {
                return type;
            }

            public void setType(WDUserPromptType type) {
                this.type = type;
            }

            public String getUserText() {
                return userText;
            }

            public void setUserText(String userText) {
                this.userText = userText;
            }

            @Override
            public String toString() {
                return "UserPromptClosedParameters{" +
                        "context='" + context + '\'' +
                        ", accepted=" + accepted +
                        ", type=" + type +
                        ", userText='" + userText + '\'' +
                        '}';
            }
        }
    }

    public static class UserPromptOpened extends WDEvent<UserPromptOpened.UserPromptOpenedParameters> {
        private String method = WDEventMapping.USER_PROMPT_OPENED.getName();

        public UserPromptOpened(JsonObject json) {
            super(json, UserPromptOpened.UserPromptOpenedParameters.class);
        }

        @Override
        public String getMethod() {
            return method;
        }

        public static class UserPromptOpenedParameters {
            private String context; // browsingContext.BrowsingContext (vermutlich eine ID als String)
            private WDUserPromptHandlerType handler; // session.UserPromptHandlerType
            private String message;
            private WDUserPromptType type;
            private String defaultValue; // Optional

            public UserPromptOpenedParameters(String context, WDUserPromptHandlerType handler, String message,
                                              WDUserPromptType type, String defaultValue) {
                this.context = context;
                this.handler = handler;
                this.message = message;
                this.type = type;
                this.defaultValue = defaultValue;
            }

            public String getContext() {
                return context;
            }

            public void setContext(String context) {
                this.context = context;
            }

            public WDUserPromptHandlerType getHandler() {
                return handler;
            }

            public void setHandler(WDUserPromptHandlerType handler) {
                this.handler = handler;
            }

            public String getMessage() {
                return message;
            }

            public void setMessage(String message) {
                this.message = message;
            }

            public WDUserPromptType getType() {
                return type;
            }

            public void setType(WDUserPromptType type) {
                this.type = type;
            }

            public String getDefaultValue() {
                return defaultValue;
            }

            public void setDefaultValue(String defaultValue) {
                this.defaultValue = defaultValue;
            }

            @Override
            public String toString() {
                return "UserPromptOpenedParameters{" +
                        "context='" + context + '\'' +
                        ", handler=" + handler +
                        ", message='" + message + '\'' +
                        ", type=" + type +
                        ", defaultValue='" + defaultValue + '\'' +
                        '}';
            }
        }
    }

    // ToDo: Check this:
    public static class HistoryUodated extends WDEvent<HistoryUodated.HistoryUpdatedParameters> {
        private String method = WDEventMapping.HISTORY_UPDATED.getName();

        public HistoryUodated(JsonObject json) {
            super(json, HistoryUpdatedParameters.class);
        }

        @Override
        public String getMethod() {
            return method;
        }

        public static class HistoryUpdatedParameters {
            private WDBrowsingContext context; // browsingContext.BrowsingContext (vermutlich eine ID als String)
            private String url;

            public HistoryUpdatedParameters(WDBrowsingContext context, String url) {
                this.context = context;
                this.url = url;
            }

            public WDBrowsingContext getContext() {
                return context;
            }

            public void setContext(WDBrowsingContext context) {
                this.context = context;
            }

            public String getUrl() {
                return url;
            }

            public void setUrl(String url) {
                this.url = url;
            }

            @Override
            public String toString() {
                return "HistoryUpdatedParameters{" +
                        "context='" + context + '\'' +
                        ", length=" + url +
                        '}';
            }
        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////



}