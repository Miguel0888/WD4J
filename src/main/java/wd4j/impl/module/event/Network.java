package wd4j.impl.module.event;

import wd4j.core.CommandImpl;
import wd4j.impl.module.generic.Module;
import wd4j.impl.module.type.*;
import wd4j.impl.module.websocket.Event;

import java.util.List;

public class Network implements Module {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Event Types (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class AuthRequired extends Event<AuthRequired.AuthRequiredParameters> {
        private String method = Method.AUTH_REQUIRED.getName();

        @Override
        public String getMethod() {
            return method;
        }

        public static class AuthRequiredParameters extends NetworkBaseParameters {
            private NetworkResponseData response;

            public AuthRequiredParameters(String context, boolean isBlocked, BrowsingContextNavigation navigation, int redirectCount,
                                          NetworkRequestData request, long timestamp, List<NetworkIntercept> intercepts,
                                          NetworkResponseData response) {
                super(context, isBlocked, navigation, redirectCount, request, timestamp, intercepts);
                this.response = response;
            }

            public NetworkResponseData getResponse() {
                return response;
            }

            public void setResponse(NetworkResponseData response) {
                this.response = response;
            }

            @Override
            public String toString() {
                return "AuthRequiredParameters{" +
                        "response=" + response +
                        "} " + super.toString();
            }
        }
    }

    public static class BeforeRequestSent extends Event<BeforeRequestSent.BeforeRequestSentParameters> {
        private String method = Method.BEFORE_REQUEST_SENT.getName();

        @Override
        public String getMethod() {
            return method;
        }

        public static class BeforeRequestSentParameters extends NetworkBaseParameters {
            private NetworkInitiator initiator;

            public BeforeRequestSentParameters(String context, boolean isBlocked, BrowsingContextNavigation navigation, int redirectCount,
                                               NetworkRequestData request, long timestamp, List<NetworkIntercept> intercepts,
                                               NetworkInitiator initiator) {
                super(context, isBlocked, navigation, redirectCount, request, timestamp, intercepts);
                this.initiator = initiator;
            }

            public NetworkInitiator getInitiator() {
                return initiator;
            }

            public void setInitiator(NetworkInitiator initiator) {
                this.initiator = initiator;
            }

            @Override
            public String toString() {
                return "BeforeRequestSentParameters{" +
                        "initiator=" + initiator +
                        "} " + super.toString();
            }
        }
    }

    public static class FetchError extends Event<FetchError.FetchErrorParameters> {
        private String method = Method.FETCH_ERROR.getName();

        @Override
        public String getMethod() {
            return method;
        }

        public static class FetchErrorParameters extends NetworkBaseParameters {
            private String errorText;

            public FetchErrorParameters(String context, boolean isBlocked, BrowsingContextNavigation navigation, int redirectCount,
                                        NetworkRequestData request, long timestamp, List<NetworkIntercept> intercepts,
                                        String errorText) {
                super(context, isBlocked, navigation, redirectCount, request, timestamp, intercepts);
                this.errorText = errorText;
            }

            public String getErrorText() {
                return errorText;
            }

            public void setErrorText(String errorText) {
                this.errorText = errorText;
            }

            @Override
            public String toString() {
                return "FetchErrorParameters{" +
                        "errorText='" + errorText + '\'' +
                        "} " + super.toString();
            }
        }
    }

    public static class ResponseCompleted extends Event<ResponseCompleted.ResponseCompletedParameters> {
        private String method = Method.RESPONSE_COMPLETED.getName();

        @Override
        public String getMethod() {
            return method;
        }

        public static class ResponseCompletedParameters extends NetworkBaseParameters {
            private NetworkResponseData response;

            public ResponseCompletedParameters(String context, boolean isBlocked, BrowsingContextNavigation navigation, int redirectCount,
                                               NetworkRequestData request, long timestamp, List<NetworkIntercept> intercepts,
                                               NetworkResponseData response) {
                super(context, isBlocked, navigation, redirectCount, request, timestamp, intercepts);
                this.response = response;
            }

            public NetworkResponseData getResponse() {
                return response;
            }

            public void setResponse(NetworkResponseData response) {
                this.response = response;
            }

            @Override
            public String toString() {
                return "ResponseCompletedParameters{" +
                        "response=" + response +
                        "} " + super.toString();
            }
        }
    }

    public static class ResponseStarted extends Event<ResponseStarted.ResponseStartedParameters> {
        private String method = Method.RESPONSE_STARTED.getName();

        @Override
        public String getMethod() {
            return method;
        }

        public static class ResponseStartedParameters extends NetworkBaseParameters {
            private NetworkResponseData response;

            public ResponseStartedParameters(String context, boolean isBlocked, BrowsingContextNavigation navigation, int redirectCount,
                                             NetworkRequestData request, long timestamp, List<NetworkIntercept> intercepts,
                                             NetworkResponseData response) {
                super(context, isBlocked, navigation, redirectCount, request, timestamp, intercepts);
                this.response = response;
            }

            public NetworkResponseData getResponse() {
                return response;
            }

            public void setResponse(NetworkResponseData response) {
                this.response = response;
            }

            @Override
            public String toString() {
                return "ResponseStartedParameters{" +
                        "response=" + response +
                        "} " + super.toString();
            }
        }
    }

}
