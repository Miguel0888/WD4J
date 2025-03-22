package wd4j.impl.dto.event;

import com.google.gson.JsonObject;
import wd4j.impl.markerInterfaces.WDModule;
import wd4j.impl.dto.type.browsingContext.WDBrowsingContext;
import wd4j.impl.dto.type.browsingContext.WDNavigation;
import wd4j.impl.dto.type.network.*;
import wd4j.impl.websocket.WDEvent;
import wd4j.impl.websocket.WDEventNames;

import java.util.List;

public class WDNetworkEvent implements WDModule {
    public WDNetworkEvent(JsonObject json) {
        // TODO: Implement mapping of JSON to Java Object
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Event Types (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class AuthRequired extends WDEvent<AuthRequired.AuthRequiredParametersWD> {
        private String method = WDEventNames.AUTH_REQUIRED.getName();

        public AuthRequired(JsonObject json) {
            super(json, AuthRequired.AuthRequiredParametersWD.class);
        }

        @Override
        public String getMethod() {
            return method;
        }

        public static class AuthRequiredParametersWD extends WDBaseParameters {
            private WDResponseData response;

            public AuthRequiredParametersWD(WDBrowsingContext context, boolean isBlocked, WDNavigation navigation, long redirectCount,
                                            WDRequestData request, long timestamp, List<WDIntercept> intercepts,
                                            WDResponseData response) {
                super(context, isBlocked, navigation, redirectCount, request, timestamp, intercepts);
                this.response = response;
            }

            public WDResponseData getResponse() {
                return response;
            }
        }
    }

    public static class BeforeRequestSent extends WDEvent<BeforeRequestSent.BeforeRequestSentParametersWD> {
        private String method = WDEventNames.BEFORE_REQUEST_SENT.getName();

        public BeforeRequestSent(JsonObject json) {
            super(json, BeforeRequestSent.BeforeRequestSentParametersWD.class);
        }

        @Override
        public String getMethod() {
            return method;
        }

        public static class BeforeRequestSentParametersWD extends WDBaseParameters {
            private WDInitiator initiator;

            public BeforeRequestSentParametersWD(WDBrowsingContext context, boolean isBlocked, WDNavigation navigation, long redirectCount,
                                                 WDRequestData request, long timestamp, List<WDIntercept> intercepts,
                                                 WDInitiator initiator) {
                super(context, isBlocked, navigation, redirectCount, request, timestamp, intercepts);
                this.initiator = initiator;
            }

            public WDInitiator getInitiator() {
                return initiator;
            }

            public void setInitiator(WDInitiator initiator) {
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

    public static class FetchError extends WDEvent<FetchError.FetchErrorParametersWD> {
        private String method = WDEventNames.FETCH_ERROR.getName();

        public FetchError(JsonObject json) {
            super(json, FetchError.FetchErrorParametersWD.class);
        }

        @Override
        public String getMethod() {
            return method;
        }

        public static class FetchErrorParametersWD extends WDBaseParameters {
            private String errorText;

            public FetchErrorParametersWD(WDBrowsingContext context, boolean isBlocked, WDNavigation navigation, long redirectCount,
                                          WDRequestData request, long timestamp, List<WDIntercept> intercepts,
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

    public static class ResponseCompleted extends WDEvent<ResponseCompleted.ResponseCompletedParametersWD> {
        private String method = WDEventNames.RESPONSE_COMPLETED.getName();

        public ResponseCompleted(JsonObject json) {
            super(json, ResponseCompleted.ResponseCompletedParametersWD.class);
        }

        @Override
        public String getMethod() {
            return method;
        }

        public static class ResponseCompletedParametersWD extends WDBaseParameters {
            private WDResponseData response;

            public ResponseCompletedParametersWD(WDBrowsingContext context, boolean isBlocked, WDNavigation navigation, long redirectCount,
                                                 WDRequestData request, long timestamp, List<WDIntercept> intercepts,
                                                 WDResponseData response) {
                super(context, isBlocked, navigation, redirectCount, request, timestamp, intercepts);
                this.response = response;
            }

            public WDResponseData getResponse() {
                return response;
            }

            public void setResponse(WDResponseData response) {
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

    public static class ResponseStarted extends WDEvent<ResponseStarted.ResponseStartedParametersWD> {
        private String method = WDEventNames.RESPONSE_STARTED.getName();

        public ResponseStarted(JsonObject json) {
            super(json, ResponseStarted.ResponseStartedParametersWD.class);
        }

        @Override
        public String getMethod() {
            return method;
        }

        public static class ResponseStartedParametersWD extends WDBaseParameters {
            private WDResponseData response;

            public ResponseStartedParametersWD(WDBrowsingContext context, boolean isBlocked, WDNavigation navigation, long redirectCount,
                                               WDRequestData request, long timestamp, List<WDIntercept> intercepts,
                                               WDResponseData response) {
                super(context, isBlocked, navigation, redirectCount, request, timestamp, intercepts);
                this.response = response;
            }

            public WDResponseData getResponse() {
                return response;
            }

            public void setResponse(WDResponseData response) {
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
