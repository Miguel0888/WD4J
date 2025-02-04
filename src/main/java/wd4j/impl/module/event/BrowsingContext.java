package wd4j.impl.module.event;

import wd4j.impl.module.generic.Module;
import wd4j.impl.module.type.*;
import wd4j.impl.module.websocket.Event;

import java.util.List;

public class BrowsingContext implements Module {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Event Types (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class Created extends Event<BrowsingContextInfo> {
        private String method = Method.CONTEXT_CREATED.name();

        @Override
        public String getMethod() {
            return method;
        }
    }


    public static class Destroyed extends Event<BrowsingContextInfo> {
        private String method = Method.CONTEXT_DESTROYED.getName();

        @Override
        public String getMethod() {
            return method;
        }
    }

    public static class NavigationStarted extends Event<BrowsingContextNavigationInfo> {
        private String method = Method.NAVIGATION_STARTED.getName();

        @Override
        public String getMethod() {
            return method;
        }
    }

    public static class FragmentNavigated extends Event<BrowsingContextNavigationInfo> {
        private String method = Method.FRAGMENT_NAVIGATED.getName();

        @Override
        public String getMethod() {
            return method;
        }
    }

    public static class DomContentLoaded extends Event<BrowsingContextNavigationInfo> {
        private String method = Method.DOM_CONTENT_LOADED.getName();

        @Override
        public String getMethod() {
            return method;
        }
    }

    public static class Load extends Event<BrowsingContextNavigationInfo> {
        private String method = Method.LOAD.getName();

        @Override
        public String getMethod() {
            return method;
        }
    }

    public static class DownloadWillBegin extends Event<BrowsingContextNavigationInfo> {
        private String method = Method.DOWNLOAD_WILL_BEGIN.getName();

        @Override
        public String getMethod() {
            return method;
        }
    }

    public static class NavigationAborted extends Event<BrowsingContextNavigationInfo> {
        private String method = Method.NAVIGATION_ABORTED.getName();

        @Override
        public String getMethod() {
            return method;
        }
    }

    public static class NavigationFailed extends Event<BrowsingContextNavigationInfo> {
        private String method = Method.NAVIGATION_FAILED.getName();

        @Override
        public String getMethod() {
            return method;
        }
    }

    public static class UserPromptClosed extends Event<UserPromptClosed.UserPromptClosedParameters> {
        private String method = Method.USER_PROMPT_CLOSED.getName();

        @Override
        public String getMethod() {
            return method;
        }

        public static class UserPromptClosedParameters {
            private String context; // browsingContext.BrowsingContext (vermutlich eine ID als String)
            private boolean accepted;
            private BrowsingContextUserPromptType type;
            private String userText; // Optional

            public UserPromptClosedParameters(String context, boolean accepted, BrowsingContextUserPromptType type, String userText) {
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

            public BrowsingContextUserPromptType getType() {
                return type;
            }

            public void setType(BrowsingContextUserPromptType type) {
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
        private String method = Method.USER_PROMPT_OPENED.getName();

        @Override
        public String getMethod() {
            return method;
        }

        public static class UserPromptOpenedParameters {
            private String context; // browsingContext.BrowsingContext (vermutlich eine ID als String)
            private SessionUserPromptHandlerType handler; // session.UserPromptHandlerType
            private String message;
            private BrowsingContextUserPromptType type;
            private String defaultValue; // Optional

            public UserPromptOpenedParameters(String context, SessionUserPromptHandlerType handler, String message,
                                              BrowsingContextUserPromptType type, String defaultValue) {
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

            public SessionUserPromptHandlerType getHandler() {
                return handler;
            }

            public void setHandler(SessionUserPromptHandlerType handler) {
                this.handler = handler;
            }

            public String getMessage() {
                return message;
            }

            public void setMessage(String message) {
                this.message = message;
            }

            public BrowsingContextUserPromptType getType() {
                return type;
            }

            public void setType(BrowsingContextUserPromptType type) {
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