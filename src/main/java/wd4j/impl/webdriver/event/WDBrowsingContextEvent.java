package wd4j.impl.webdriver.event;

import wd4j.impl.markerInterfaces.WDModule;
import wd4j.impl.webdriver.type.browsingContext.WDInfo;
import wd4j.impl.webdriver.type.browsingContext.WDNavigationInfo;
import wd4j.impl.webdriver.type.browsingContext.WDUserPromptType;
import wd4j.impl.webdriver.type.session.WDUserPromptHandlerType;
import wd4j.impl.websocket.WDEvent;

public class WDBrowsingContextEvent implements WDModule {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Event Types (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class Created extends WDEvent<WDInfo> {
        private String method = WDMethodEvent.CONTEXT_CREATED.name();

        @Override
        public String getMethod() {
            return method;
        }
    }


    public static class Destroyed extends WDEvent<WDInfo> {
        private String method = WDMethodEvent.CONTEXT_DESTROYED.getName();

        @Override
        public String getMethod() {
            return method;
        }
    }

    public static class NavigationStarted extends WDEvent<WDNavigationInfo> {
        private String method = WDMethodEvent.NAVIGATION_STARTED.getName();

        @Override
        public String getMethod() {
            return method;
        }
    }

    public static class FragmentNavigated extends WDEvent<WDNavigationInfo> {
        private String method = WDMethodEvent.FRAGMENT_NAVIGATED.getName();

        @Override
        public String getMethod() {
            return method;
        }
    }

    public static class DomContentLoaded extends WDEvent<WDNavigationInfo> {
        private String method = WDMethodEvent.DOM_CONTENT_LOADED.getName();

        @Override
        public String getMethod() {
            return method;
        }
    }

    public static class Load extends WDEvent<WDNavigationInfo> {
        private String method = WDMethodEvent.LOAD.getName();

        @Override
        public String getMethod() {
            return method;
        }
    }

    public static class DownloadWillBegin extends WDEvent<WDNavigationInfo> {
        private String method = WDMethodEvent.DOWNLOAD_WILL_BEGIN.getName();

        @Override
        public String getMethod() {
            return method;
        }
    }

    public static class NavigationAborted extends WDEvent<WDNavigationInfo> {
        private String method = WDMethodEvent.NAVIGATION_ABORTED.getName();

        @Override
        public String getMethod() {
            return method;
        }
    }

    public static class NavigationFailed extends WDEvent<WDNavigationInfo> {
        private String method = WDMethodEvent.NAVIGATION_FAILED.getName();

        @Override
        public String getMethod() {
            return method;
        }
    }

    public static class UserPromptClosed extends WDEvent<UserPromptClosed.UserPromptClosedParameters> {
        private String method = WDMethodEvent.USER_PROMPT_CLOSED.getName();

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
        private String method = WDMethodEvent.USER_PROMPT_OPENED.getName();

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


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////



}