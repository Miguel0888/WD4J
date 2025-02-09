package wd4j.impl.webdriver.command.response;

import wd4j.impl.markerInterfaces.ResultData;
import wd4j.impl.webdriver.type.session.Subscription;

import java.util.Map;

public interface SessionResult extends ResultData {
    class NewResult implements SessionResult {
        private final String sessionId;
        private final Map<String, Object> capabilities;

        public NewResult(String sessionId, Map<String, Object> capabilities) {
            this.sessionId = sessionId;
            this.capabilities = capabilities;
        }

        public String getSessionId() {
            return sessionId;
        }

        public Map<String, Object> getCapabilities() {
            return capabilities;
        }

        @Override
        public String toString() {
            return "SessionNewResult{" +
                    "sessionId='" + sessionId + '\'' +
                    ", capabilities=" + capabilities +
                    '}';
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
