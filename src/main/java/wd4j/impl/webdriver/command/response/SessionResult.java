package wd4j.impl.webdriver.command.response;

import wd4j.impl.markerInterfaces.ResultData;
import wd4j.impl.webdriver.type.session.ProxyConfiguration;
import wd4j.impl.webdriver.type.session.Subscription;
import wd4j.impl.webdriver.type.session.UserPromptHandler;

import java.util.Map;

public interface SessionResult extends ResultData {
    public static class NewResult implements SessionResult {
        private String sessionId;
        private Capabilities capabilities;

        // 🔥 WICHTIG: Gson braucht diesen Konstruktor!
        public NewResult() {}

        public String getSessionId() {
            return sessionId;
        }

        public Capabilities getCapabilities() {
            return capabilities;
        }

        @Override
        public String toString() {
            return "SessionNewResult{" +
                    "sessionId='" + sessionId + '\'' +
                    ", capabilities=" + capabilities +
                    '}';
        }
        public static class Capabilities {
            private boolean acceptInsecureCerts;
            private String browserName;
            private String browserVersion;
            private String platformName;
            private boolean setWindowRect;
            private String userAgent;

            private ProxyConfiguration proxy; // Optional
            private UserPromptHandler unhandledPromptBehavior; // Optional
            private String webSocketUrl; // Optional

            public boolean isAcceptInsecureCerts() {
                return acceptInsecureCerts;
            }

            public String getBrowserName() {
                return browserName;
            }

            public String getBrowserVersion() {
                return browserVersion;
            }

            public String getPlatformName() {
                return platformName;
            }

            public boolean isSetWindowRect() {
                return setWindowRect;
            }

            public String getUserAgent() {
                return userAgent;
            }

            public ProxyConfiguration getProxy() {
                return proxy;
            }

            public UserPromptHandler getUnhandledPromptBehavior() {
                return unhandledPromptBehavior;
            }

            public String getWebSocketUrl() {
                return webSocketUrl;
            }

            @Override
            public String toString() {
                return "Capabilities{" +
                        "acceptInsecureCerts=" + acceptInsecureCerts +
                        ", browserName='" + browserName + '\'' +
                        ", browserVersion='" + browserVersion + '\'' +
                        ", platformName='" + platformName + '\'' +
                        ", setWindowRect=" + setWindowRect +
                        ", userAgent='" + userAgent + '\'' +
                        ", proxy=" + proxy +
                        ", unhandledPromptBehavior=" + unhandledPromptBehavior +
                        ", webSocketUrl='" + webSocketUrl + '\'' +
                        '}';
            }
        }
    }

    class StatusResult implements SessionResult {
        private final boolean ready;
        private final String message;

        public StatusResult(boolean ready, String message) {
            this.ready = ready;
            this.message = message;
        }

        public boolean isReady() {
            return ready;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return "SessionStatusResult{" +
                    "ready=" + ready +
                    ", message='" + message + '\'' +
                    '}';
        }
    }

    class SubscribeResult implements SessionResult {
        private final Subscription subscription;

        public SubscribeResult(Subscription subscription) {
            this.subscription = subscription;
        }

        public Subscription getSubscription() {
            return subscription;
        }

        @Override
        public String toString() {
            return "SessionSubscribeResult{" +
                    "subscription=" + subscription +
                    '}';
        }
    }
}
