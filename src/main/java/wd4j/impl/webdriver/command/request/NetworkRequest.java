package wd4j.impl.webdriver.command.request;

import wd4j.impl.markerInterfaces.CommandData;
import wd4j.impl.webdriver.command.request.helper.CommandImpl;
import wd4j.impl.webdriver.command.request.parameters.network.*;
import wd4j.impl.webdriver.command.request.parameters.network.CookieHeader;
import wd4j.impl.webdriver.type.browsingContext.BrowsingContext;
import wd4j.impl.webdriver.type.network.*;
import wd4j.impl.websocket.Command;

import java.util.List;

public class NetworkRequest {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Commands (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class AddIntercept extends CommandImpl<AddInterceptParameters> implements CommandData {
        public AddIntercept(List<AddInterceptParameters.InterceptPhase> phases) {
            super("network.addIntercept", new AddInterceptParameters(phases));
        }
        public AddIntercept(List<AddInterceptParameters.InterceptPhase> phases, List<BrowsingContext> contexts, List<UrlPattern> urlPattern) {
            super("network.addIntercept", new AddInterceptParameters(phases, contexts, urlPattern));
        }
    }

    public static class ContinueRequest extends CommandImpl<ContinueRequestParameters> implements CommandData {
        public ContinueRequest(String requestId) {
            super("network.continueRequest", new ContinueRequestParameters(new Request(requestId)));
        }
        public ContinueRequest(Request request) {
            super("network.continueRequest", new ContinueRequestParameters(request));
        }
        public ContinueRequest(Request request, BytesValue body, List<CookieHeader> cookies, List<Header> headers, String method, String url) {
            super("network.continueRequest", new ContinueRequestParameters(request, body, cookies, headers, method, url));
        }
    }

    public static class ContinueResponse extends CommandImpl<ContinueResponseParameters> implements CommandData {
        public ContinueResponse(String requestId) {
            super("network.continueResponse", new ContinueResponseParameters(new Request(requestId)));
        }
        public ContinueResponse(Request request) {
            super("network.continueResponse", new ContinueResponseParameters(request));
        }
        public ContinueResponse(Request request, List<SetCookieHeader> cookies, AuthCredentials rawResponse, List<Header> responseHeaders, String text, Integer statusCode) {
            super("network.continueResponse", new ContinueResponseParameters(request, cookies, rawResponse, responseHeaders, text, statusCode));
        }
    }

    public static class ContinueWithAuth extends CommandImpl<ContinueWithAuthParameters> implements CommandData {
        public ContinueWithAuth(String requestId, AuthCredentials authChallengeResponse) {
            super("network.continueWithAuth", new ContinueWithAuthCredentials(new Request(requestId), authChallengeResponse));
        }
        public ContinueWithAuth(Request request, AuthCredentials authChallengeResponse) {
            super("network.continueWithAuth", new ContinueWithAuthCredentials(request, authChallengeResponse));
        }
        public ContinueWithAuth(Request request, ContinueWithAuthNoCredentials.Action action) {
            super("network.continueWithAuth", new ContinueWithAuthNoCredentials(request, action));
        }
    }

    public static class FailRequest extends CommandImpl<FailRequestParameters> implements CommandData {
        public FailRequest(String requestId) {
            super("network.failRequest", new FailRequestParameters(new Request(requestId)));
        }
        public FailRequest(Request request) {
            super("network.failRequest", new FailRequestParameters(request));
        }
    }

    public static class ProvideResponse extends CommandImpl<ProvideResponseParameters> implements CommandData {
        public ProvideResponse(String requestId) {
            super("network.provideResponse", new ProvideResponseParameters(new Request(requestId)));
        }
        public ProvideResponse(Request request) {
            super("network.provideResponse", new ProvideResponseParameters(request));
        }
        public ProvideResponse(Request request, BytesValue body, List<SetCookieHeader> cookies, List<Header> headers, String reasonPhrase, Integer statusCode) {
            super("network.provideResponse", new ProvideResponseParameters(request, body, cookies, headers, reasonPhrase, statusCode));
        }
    }

    public static class RemoveIntercept extends CommandImpl<RemoveInterceptParameters> implements CommandData {
        public RemoveIntercept(String interceptId) {
            super("network.removeIntercept", new RemoveInterceptParameters(new Intercept(interceptId)));
        }
        public RemoveIntercept(Intercept intercept) {
            super("network.removeIntercept", new RemoveInterceptParameters(intercept));
        }
    }

    public static class SetCacheBehavior extends CommandImpl<SetCacheBehaviorParameters> implements CommandData {
        public SetCacheBehavior(SetCacheBehaviorParameters.CacheBehavior cacheBehavior) {
            super("network.setCacheBehavior", new SetCacheBehaviorParameters(cacheBehavior));
        }
        public SetCacheBehavior(SetCacheBehaviorParameters.CacheBehavior cacheBehavior, List<BrowsingContext> contexts) {
            super("network.setCacheBehavior", new SetCacheBehaviorParameters(cacheBehavior, contexts));
        }
    }

}