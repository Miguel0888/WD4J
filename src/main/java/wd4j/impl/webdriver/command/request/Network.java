package wd4j.impl.webdriver.command.request;

import wd4j.impl.markerInterfaces.CommandData;
import wd4j.impl.websocket.Command;

public class Network {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Commands (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class AddIntercept extends CommandImpl<AddIntercept.ParamsImpl> implements CommandData {

        public AddIntercept(String urlPattern) {
            super("network.addIntercept", new ParamsImpl(urlPattern));
        }

        public static class ParamsImpl implements Command.Params {
            private final String urlPattern;

            public ParamsImpl(String urlPattern) {
                if (urlPattern == null || urlPattern.isEmpty()) {
                    throw new IllegalArgumentException("URL pattern must not be null or empty.");
                }
                this.urlPattern = urlPattern;
            }
        }
    }

    public static class ContinueRequest extends CommandImpl<ContinueRequest.ParamsImpl> implements CommandData {

        public ContinueRequest(String requestId) {
            super("network.continueRequest", new ParamsImpl(requestId));
        }

        public static class ParamsImpl implements Command.Params {
            private final String requestId;

            public ParamsImpl(String requestId) {
                if (requestId == null || requestId.isEmpty()) {
                    throw new IllegalArgumentException("Request ID must not be null or empty.");
                }
                this.requestId = requestId;
            }
        }
    }

    public static class ContinueResponse extends CommandImpl<ContinueResponse.ParamsImpl> implements CommandData {

        public ContinueResponse(String requestId) {
            super("network.continueResponse", new ParamsImpl(requestId));
        }

        public static class ParamsImpl implements Command.Params {
            private final String requestId;

            public ParamsImpl(String requestId) {
                if (requestId == null || requestId.isEmpty()) {
                    throw new IllegalArgumentException("Request ID must not be null or empty.");
                }
                this.requestId = requestId;
            }
        }
    }

    public static class ContinueWithAuth extends CommandImpl<ContinueWithAuth.ParamsImpl> implements CommandData {

        public ContinueWithAuth(String requestId, String authChallengeResponse) {
            super("network.continueWithAuth", new ParamsImpl(requestId, authChallengeResponse));
        }

        public static class ParamsImpl implements Command.Params {
            private final String requestId;
            private final String authChallengeResponse;

            public ParamsImpl(String requestId, String authChallengeResponse) {
                if (requestId == null || requestId.isEmpty()) {
                    throw new IllegalArgumentException("Request ID must not be null or empty.");
                }
                if (authChallengeResponse == null || authChallengeResponse.isEmpty()) {
                    throw new IllegalArgumentException("Auth challenge response must not be null or empty.");
                }
                this.requestId = requestId;
                this.authChallengeResponse = authChallengeResponse;
            }
        }
    }

    public static class FailRequest extends CommandImpl<FailRequest.ParamsImpl> implements CommandData {

        public FailRequest(String requestId) {
            super("network.failRequest", new ParamsImpl(requestId));
        }

        public static class ParamsImpl implements Command.Params {
            private final String requestId;

            public ParamsImpl(String requestId) {
                if (requestId == null || requestId.isEmpty()) {
                    throw new IllegalArgumentException("Request ID must not be null or empty.");
                }
                this.requestId = requestId;
            }
        }
    }

    public static class ProvideResponse extends CommandImpl<ProvideResponse.ParamsImpl> implements CommandData {

        public ProvideResponse(String requestId, String responseBody) {
            super("network.provideResponse", new ParamsImpl(requestId, responseBody));
        }

        public static class ParamsImpl implements Command.Params {
            private final String requestId;
            private final String responseBody;

            public ParamsImpl(String requestId, String responseBody) {
                if (requestId == null || requestId.isEmpty()) {
                    throw new IllegalArgumentException("Request ID must not be null or empty.");
                }
                if (responseBody == null || responseBody.isEmpty()) {
                    throw new IllegalArgumentException("Response body must not be null or empty.");
                }
                this.requestId = requestId;
                this.responseBody = responseBody;
            }
        }
    }

    public static class RemoveIntercept extends CommandImpl<RemoveIntercept.ParamsImpl> implements CommandData {

        public RemoveIntercept(String interceptId) {
            super("network.removeIntercept", new ParamsImpl(interceptId));
        }

        public static class ParamsImpl implements Command.Params {
            private final String interceptId;

            public ParamsImpl(String interceptId) {
                if (interceptId == null || interceptId.isEmpty()) {
                    throw new IllegalArgumentException("Intercept ID must not be null or empty.");
                }
                this.interceptId = interceptId;
            }
        }
    }

    public static class SetCacheBehavior extends CommandImpl<SetCacheBehavior.ParamsImpl> implements CommandData {

        public SetCacheBehavior(String contextId, String cacheMode) {
            super("network.setCacheBehavior", new ParamsImpl(contextId, cacheMode));
        }

        public static class ParamsImpl implements Command.Params {
            private final String contextId;
            private final String cacheMode;

            public ParamsImpl(String contextId, String cacheMode) {
                if (contextId == null || contextId.isEmpty()) {
                    throw new IllegalArgumentException("Context ID must not be null or empty.");
                }
                if (cacheMode == null || cacheMode.isEmpty()) {
                    throw new IllegalArgumentException("Cache mode must not be null or empty.");
                }
                this.contextId = contextId;
                this.cacheMode = cacheMode;
            }
        }
    }
}