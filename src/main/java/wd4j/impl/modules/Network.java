package wd4j.impl.modules;

import wd4j.core.CommandImpl;
import wd4j.core.WebSocketConnection;
import wd4j.impl.generic.Command;
import wd4j.impl.generic.Module;

import java.util.List;

public class Network implements Module {

    private final WebSocketConnection webSocketConnection;

    public Network(WebSocketConnection webSocketConnection) {
        this.webSocketConnection = webSocketConnection;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Adds an intercept for network requests matching the given URL pattern.
     *
     * @param urlPattern The URL pattern to intercept.
     * @throws RuntimeException if the operation fails.
     */
    public void addIntercept(String urlPattern) {
        try {
            webSocketConnection.send(new AddIntercept(urlPattern));
            System.out.println("Intercept added for URL pattern: " + urlPattern);
        } catch (RuntimeException e) {
            System.out.println("Error adding intercept: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Continues a network request with the given request ID.
     *
     * @param requestId The ID of the request to continue.
     * @throws RuntimeException if the operation fails.
     */
    public void continueRequest(String requestId) {
        try {
            webSocketConnection.send(new ContinueRequest(requestId));
            System.out.println("Request continued: " + requestId);
        } catch (RuntimeException e) {
            System.out.println("Error continuing request: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Continues a network response with the given request ID.
     *
     * @param requestId The ID of the response to continue.
     * @throws RuntimeException if the operation fails.
     */
    public void continueResponse(String requestId) {
        try {
            webSocketConnection.send(new ContinueResponse(requestId));
            System.out.println("Response continued: " + requestId);
        } catch (RuntimeException e) {
            System.out.println("Error continuing response: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Continues a network request with authentication challenge response.
     *
     * @param requestId              The ID of the request.
     * @param authChallengeResponse  The authentication challenge response.
     * @throws RuntimeException if the operation fails.
     */
    public void continueWithAuth(String requestId, String authChallengeResponse) {
        try {
            webSocketConnection.send(new ContinueWithAuth(requestId, authChallengeResponse));
            System.out.println("Request continued with authentication: " + requestId);
        } catch (RuntimeException e) {
            System.out.println("Error continuing with authentication: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Fails a network request with the given request ID.
     *
     * @param requestId The ID of the request to fail.
     * @throws RuntimeException if the operation fails.
     */
    public void failRequest(String requestId) {
        try {
            webSocketConnection.send(new FailRequest(requestId));
            System.out.println("Request failed: " + requestId);
        } catch (RuntimeException e) {
            System.out.println("Error failing request: " + e.getMessage());
            throw e;
        }
    }


    /**
     * Provides a custom response for a network request.
     *
     * @param requestId   The ID of the request to respond to.
     * @param responseBody The custom response body.
     * @throws RuntimeException if the operation fails.
     */
    public void provideResponse(String requestId, String responseBody) {
        try {
            webSocketConnection.send(new ProvideResponse(requestId, responseBody));
            System.out.println("Response provided for request: " + requestId);
        } catch (RuntimeException e) {
            System.out.println("Error providing response: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Removes a previously added intercept.
     *
     * @param interceptId The ID of the intercept to remove.
     * @throws RuntimeException if the operation fails.
     */
    public void removeIntercept(String interceptId) {
        try {
            webSocketConnection.send(new RemoveIntercept(interceptId));
            System.out.println("Intercept removed: " + interceptId);
        } catch (RuntimeException e) {
            System.out.println("Error removing intercept: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Sets the cache behavior for the given context.
     *
     * @param contextId The ID of the context.
     * @param cacheMode The cache mode to set (e.g., "no-store").
     * @throws RuntimeException if the operation fails.
     */
    public void setCacheBehavior(String contextId, String cacheMode) {
        try {
            webSocketConnection.send(new SetCacheBehavior(contextId, cacheMode));
            System.out.println("Cache behavior set for context: " + contextId + " -> " + cacheMode);
        } catch (RuntimeException e) {
            System.out.println("Error setting cache behavior: " + e.getMessage());
            throw e;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Types (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public class NetworkAuthChallenge {
        private final String source;
        private final String origin;

        public NetworkAuthChallenge(String source, String origin) {
            this.source = source;
            this.origin = origin;
        }

        public String getSource() {
            return source;
        }

        public String getOrigin() {
            return origin;
        }
    }

    public class NetworkAuthCredentials {
        private final String username;
        private final String password;

        public NetworkAuthCredentials(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }
    }

    public class NetworkBaseParameters {
        private final String contextId;

        public NetworkBaseParameters(String contextId) {
            this.contextId = contextId;
        }

        public String getContextId() {
            return contextId;
        }
    }

    public class NetworkBytesValue {
        private final int size;

        public NetworkBytesValue(int size) {
            this.size = size;
        }

        public int getSize() {
            return size;
        }
    }

    public class NetworkCookie {
        private final String name;
        private final String value;

        public NetworkCookie(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }
    }

    public class NetworkCookieHeader {
        private final List<NetworkCookie> cookies;

        public NetworkCookieHeader(List<NetworkCookie> cookies) {
            this.cookies = cookies;
        }

        public List<NetworkCookie> getCookies() {
            return cookies;
        }
    }

    public class NetworkFetchTimingInfo {
        private final long startTime;
        private final long endTime;

        public NetworkFetchTimingInfo(long startTime, long endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }

        public long getStartTime() {
            return startTime;
        }

        public long getEndTime() {
            return endTime;
        }
    }

    public class NetworkHeader {
        private final String name;
        private final String value;

        public NetworkHeader(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }
    }

    public class NetworkInitiator {
        private final String type;

        public NetworkInitiator(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }

    public class NetworkIntercept {
        private final String id;

        public NetworkIntercept(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    public class NetworkRequest {
        private final String url;
        private final String method;

        public NetworkRequest(String url, String method) {
            this.url = url;
            this.method = method;
        }

        public String getUrl() {
            return url;
        }

        public String getMethod() {
            return method;
        }
    }

    public class NetworkRequestData {
        private final String url;

        public NetworkRequestData(String url) {
            this.url = url;
        }

        public String getUrl() {
            return url;
        }
    }

    public class NetworkResponseContent {
        private final String content;

        public NetworkResponseContent(String content) {
            this.content = content;
        }

        public String getContent() {
            return content;
        }
    }

    public class NetworkResponseData {
        private final int status;
        private final String statusText;

        public NetworkResponseData(int status, String statusText) {
            this.status = status;
            this.statusText = statusText;
        }

        public int getStatus() {
            return status;
        }

        public String getStatusText() {
            return statusText;
        }
    }

    public class NetworkSetCookieHeader {
        private final String name;
        private final String value;

        public NetworkSetCookieHeader(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }
    }

    public class NetworkUrlPattern {
        private final String pattern;

        public NetworkUrlPattern(String pattern) {
            this.pattern = pattern;
        }

        public String getPattern() {
            return pattern;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Events (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Commands (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class AddIntercept extends CommandImpl<AddIntercept.ParamsImpl> {

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

    public static class ContinueRequest extends CommandImpl<ContinueRequest.ParamsImpl> {

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

    public static class ContinueResponse extends CommandImpl<ContinueResponse.ParamsImpl> {

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

    public static class ContinueWithAuth extends CommandImpl<ContinueWithAuth.ParamsImpl> {

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

    public static class FailRequest extends CommandImpl<FailRequest.ParamsImpl> {

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

    public static class ProvideResponse extends CommandImpl<ProvideResponse.ParamsImpl> {

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

    public static class RemoveIntercept extends CommandImpl<RemoveIntercept.ParamsImpl> {

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

    public static class SetCacheBehavior extends CommandImpl<SetCacheBehavior.ParamsImpl> {

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