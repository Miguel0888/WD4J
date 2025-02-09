package wd4j.impl.webdriver.event;

import wd4j.impl.markerInterfaces.Module;
import wd4j.impl.webdriver.type.browsingContext.BrowsingContext;
import wd4j.impl.webdriver.type.browsingContext.Navigation;
import wd4j.impl.webdriver.type.network.*;
import wd4j.impl.websocket.Event;

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

        public static class AuthRequiredParameters extends BaseParameters {
            private ResponseData response;

            public AuthRequiredParameters(BrowsingContext context, boolean isBlocked, Navigation navigation, char redirectCount,
                                          RequestData request, long timestamp, List<Intercept> intercepts,
                                          ResponseData response) {
                super(context, isBlocked, navigation, redirectCount, request, timestamp, intercepts);
                this.response = response;
            }

            public ResponseData getResponse() {
                return response;
            }
        }
    }

    public static class BeforeRequestSent extends Event<BeforeRequestSent.BeforeRequestSentParameters> {
        private String method = Method.BEFORE_REQUEST_SENT.getName();

        @Override
        public String getMethod() {
            return method;
        }

        public static class BeforeRequestSentParameters extends BaseParameters {
            private Initiator initiator;

            public BeforeRequestSentParameters(BrowsingContext context, boolean isBlocked, Navigation navigation, char redirectCount,
                                               RequestData request, long timestamp, List<Intercept> intercepts,
                                               Initiator initiator) {
                super(context, isBlocked, navigation, redirectCount, request, timestamp, intercepts);
                this.initiator = initiator;
            }

            public Initiator getInitiator() {
                return initiator;
            }

            public void setInitiator(Initiator initiator) {
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

        public static class FetchErrorParameters extends BaseParameters {
            private String errorText;

            public FetchErrorParameters(BrowsingContext context, boolean isBlocked, Navigation navigation, char redirectCount,
                                        RequestData request, long timestamp, List<Intercept> intercepts,
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

        public static class ResponseCompletedParameters extends BaseParameters {
            private ResponseData response;

            public ResponseCompletedParameters(BrowsingContext context, boolean isBlocked, Navigation navigation, char redirectCount,
                                               RequestData request, long timestamp, List<Intercept> intercepts,
                                               ResponseData response) {
                super(context, isBlocked, navigation, redirectCount, request, timestamp, intercepts);
                this.response = response;
            }

            public ResponseData getResponse() {
                return response;
            }

            public void setResponse(ResponseData response) {
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

        public static class ResponseStartedParameters extends BaseParameters {
            private ResponseData response;

            public ResponseStartedParameters(BrowsingContext context, boolean isBlocked, Navigation navigation, char redirectCount,
                                             RequestData request, long timestamp, List<Intercept> intercepts,
                                             ResponseData response) {
                super(context, isBlocked, navigation, redirectCount, request, timestamp, intercepts);
                this.response = response;
            }

            public ResponseData getResponse() {
                return response;
            }

            public void setResponse(ResponseData response) {
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
