package wd4j.impl.webdriver.command.response;

import wd4j.impl.markerInterfaces.WDResultData;
import wd4j.impl.webdriver.type.session.WDProxyConfiguration;
import wd4j.impl.webdriver.type.session.WDSubscription;

public interface WDSessionResult extends WDResultData {
    public static class NewSessionResult implements WDSessionResult {
        private String sessionId;
        private Capabilities capabilities;

        // 🔥 WICHTIG: Gson braucht diesen Konstruktor!
        public NewSessionResult() {}

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

            private WDProxyConfiguration proxy; // Optional
            @Deprecated // since it should return WDUserPromptHandler Object instead of the String
            private String unhandledPromptBehavior;
            //            private WDUserPromptHandler unhandledPromptBehavior; // Optional
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

            public WDProxyConfiguration getProxy() {
                return proxy;
            }

            @Deprecated // since it should return WDUserPromptHandler Object instead of the String
            public String getUnhandledPromptBehavior() {
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

    class StatusSessionResult implements WDSessionResult {
        private final boolean ready;
        private final String message;

        public StatusSessionResult(boolean ready, String message) {
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

    class SubscribeSessionResult implements WDSessionResult {
        private final WDSubscription WDSubscription;

        public SubscribeSessionResult(WDSubscription WDSubscription) {
            this.WDSubscription = WDSubscription;
        }

        public WDSubscription getSubscription() {
            return WDSubscription;
        }

        @Override
        public String toString() {
            return "SessionSubscribeResult{" +
                    "subscription=" + WDSubscription +
                    '}';
        }
    }
}
