package wd4j.impl.webdriver.event;

import wd4j.impl.markerInterfaces.Module;
import wd4j.impl.webdriver.type.browsingContext.Info;
import wd4j.impl.webdriver.type.browsingContext.NavigationInfo;
import wd4j.impl.webdriver.type.browsingContext.UserPromptType;
import wd4j.impl.webdriver.type.session.UserPromptHandlerType;
import wd4j.impl.websocket.Event;

public class BrowsingContextEvent implements Module {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Event Types (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class Created extends Event<Info> {
        private String method = MethodEvent.CONTEXT_CREATED.name();

        @Override
        public String getMethod() {
            return method;
        }
    }


    public static class Destroyed extends Event<Info> {
        private String method = MethodEvent.CONTEXT_DESTROYED.getName();

        @Override
        public String getMethod() {
            return method;
        }
    }

    public static class NavigationStarted extends Event<NavigationInfo> {
        private String method = MethodEvent.NAVIGATION_STARTED.getName();

        @Override
        public String getMethod() {
            return method;
        }
    }

    public static class FragmentNavigated extends Event<NavigationInfo> {
        private String method = MethodEvent.FRAGMENT_NAVIGATED.getName();

        @Override
        public String getMethod() {
            return method;
        }
    }

    public static class DomContentLoaded extends Event<NavigationInfo> {
        private String method = MethodEvent.DOM_CONTENT_LOADED.getName();

        @Override
        public String getMethod() {
            return method;
        }
    }

    public static class Load extends Event<NavigationInfo> {
        private String method = MethodEvent.LOAD.getName();

        @Override
        public String getMethod() {
            return method;
        }
    }

    public static class DownloadWillBegin extends Event<NavigationInfo> {
        private String method = MethodEvent.DOWNLOAD_WILL_BEGIN.getName();

        @Override
        public String getMethod() {
            return method;
        }
    }

    public static class NavigationAborted extends Event<NavigationInfo> {
        private String method = MethodEvent.NAVIGATION_ABORTED.getName();

        @Override
        public String getMethod() {
            return method;
        }
    }

    public static class NavigationFailed extends Event<NavigationInfo> {
        private String method = MethodEvent.NAVIGATION_FAILED.getName();

        @Override
        public String getMethod() {
            return method;
        }
    }

    public static class UserPromptClosed extends Event<UserPromptClosed.UserPromptClosedParameters> {
        private String method = MethodEvent.USER_PROMPT_CLOSED.getName();

        @Override
        public String getMethod() {
            return method;
        }

        public static class UserPromptClosedParameters {
            private String context; // browsingContext.BrowsingContext (vermutlich eine ID als String)
            private boolean accepted;
            private UserPromptType type;
            private String userText; // Optional

            public UserPromptClosedParameters(String context, boolean accepted, UserPromptType type, String userText) {
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

            public UserPromptType getType() {
                return type;
            }

            public void setType(UserPromptType type) {
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

    public static class UserPromptOpened extends Event<UserPromptOpened.UserPromptOpenedParameters> {
        private String method = MethodEvent.USER_PROMPT_OPENED.getName();

        @Override
        public String getMethod() {
            return method;
        }

        public static class UserPromptOpenedParameters {
            private String context; // browsingContext.BrowsingContext (vermutlich eine ID als String)
            private UserPromptHandlerType handler; // session.UserPromptHandlerType
            private String message;
            private UserPromptType type;
            private String defaultValue; // Optional

            public UserPromptOpenedParameters(String context, UserPromptHandlerType handler, String message,
                                              UserPromptType type, String defaultValue) {
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

            public UserPromptHandlerType getHandler() {
                return handler;
            }

            public void setHandler(UserPromptHandlerType handler) {
                this.handler = handler;
            }

            public String getMessage() {
                return message;
            }

            public void setMessage(String message) {
                this.message = message;
            }

            public UserPromptType getType() {
                return type;
            }

            public void setType(UserPromptType type) {
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