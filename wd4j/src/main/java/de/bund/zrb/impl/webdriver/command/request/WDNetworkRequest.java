package de.bund.zrb.impl.webdriver.command.request;

import de.bund.zrb.impl.markerInterfaces.WDCommandData;
import de.bund.zrb.impl.webdriver.command.request.helper.WDCommandImpl;
import de.bund.zrb.impl.webdriver.command.request.parameters.network.*;
import de.bund.zrb.impl.webdriver.type.network.*;
import wd4j.impl.webdriver.command.request.parameters.network.*;
import de.bund.zrb.impl.webdriver.type.browsingContext.WDBrowsingContext;
import wd4j.impl.webdriver.type.network.*;

import java.util.List;

public class WDNetworkRequest {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Commands (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class AddIntercept extends WDCommandImpl<AddInterceptParameters> implements WDCommandData {
        public AddIntercept(List<AddInterceptParameters.InterceptPhase> phases) {
            super("network.addIntercept", new AddInterceptParameters(phases));
        }
        public AddIntercept(List<AddInterceptParameters.InterceptPhase> phases, List<WDBrowsingContext> contexts, List<WDUrlPattern> WDUrlPattern) {
            super("network.addIntercept", new AddInterceptParameters(phases, contexts, WDUrlPattern));
        }
    }

    public static class ContinueRequest extends WDCommandImpl<ContinueRequestParameters> implements WDCommandData {
        public ContinueRequest(String requestId) {
            super("network.continueRequest", new ContinueRequestParameters(new WDRequest(requestId)));
        }
        public ContinueRequest(WDRequest WDRequest) {
            super("network.continueRequest", new ContinueRequestParameters(WDRequest));
        }
        public ContinueRequest(WDRequest WDRequest, WDBytesValue body, List<CookieHeader> cookies, List<WDHeader> WDHeaders, String method, String url) {
            super("network.continueRequest", new ContinueRequestParameters(WDRequest, body, cookies, WDHeaders, method, url));
        }
    }

    public static class ContinueResponse extends WDCommandImpl<ContinueResponseParameters> implements WDCommandData {
        public ContinueResponse(String requestId) {
            super("network.continueResponse", new ContinueResponseParameters(new WDRequest(requestId)));
        }
        public ContinueResponse(WDRequest WDRequest) {
            super("network.continueResponse", new ContinueResponseParameters(WDRequest));
        }
        public ContinueResponse(WDRequest WDRequest, List<WDSetCookieHeader> cookies, WDAuthCredentials rawResponse, List<WDHeader> responseWDHeaders, String text, Integer statusCode) {
            super("network.continueResponse", new ContinueResponseParameters(WDRequest, cookies, rawResponse, responseWDHeaders, text, statusCode));
        }
    }

    public static class ContinueWithAuth extends WDCommandImpl<ContinueWithAuthParameters> implements WDCommandData {
        public ContinueWithAuth(String requestId, WDAuthCredentials authChallengeResponse) {
            super("network.continueWithAuth", new ContinueWithAuthCredentials(new WDRequest(requestId), authChallengeResponse));
        }
        public ContinueWithAuth(WDRequest WDRequest, WDAuthCredentials authChallengeResponse) {
            super("network.continueWithAuth", new ContinueWithAuthCredentials(WDRequest, authChallengeResponse));
        }
        public ContinueWithAuth(WDRequest WDRequest, ContinueWithAuthNoCredentials.Action action) {
            super("network.continueWithAuth", new ContinueWithAuthNoCredentials(WDRequest, action));
        }
    }

    public static class FailRequest extends WDCommandImpl<FailRequestParameters> implements WDCommandData {
        public FailRequest(String requestId) {
            super("network.failRequest", new FailRequestParameters(new WDRequest(requestId)));
        }
        public FailRequest(WDRequest WDRequest) {
            super("network.failRequest", new FailRequestParameters(WDRequest));
        }
    }

    public static class ProvideResponse extends WDCommandImpl<ProvideResponseParameters> implements WDCommandData {
        public ProvideResponse(String requestId) {
            super("network.provideResponse", new ProvideResponseParameters(new WDRequest(requestId)));
        }
        public ProvideResponse(WDRequest WDRequest) {
            super("network.provideResponse", new ProvideResponseParameters(WDRequest));
        }
        public ProvideResponse(WDRequest WDRequest, WDBytesValue body, List<WDSetCookieHeader> cookies, List<WDHeader> WDHeaders, String reasonPhrase, Integer statusCode) {
            super("network.provideResponse", new ProvideResponseParameters(WDRequest, body, cookies, WDHeaders, reasonPhrase, statusCode));
        }
    }

    public static class RemoveIntercept extends WDCommandImpl<RemoveInterceptParameters> implements WDCommandData {
        public RemoveIntercept(String interceptId) {
            super("network.removeIntercept", new RemoveInterceptParameters(new WDIntercept(interceptId)));
        }
        public RemoveIntercept(WDIntercept WDIntercept) {
            super("network.removeIntercept", new RemoveInterceptParameters(WDIntercept));
        }
    }

    public static class SetCacheBehavior extends WDCommandImpl<SetCacheBehaviorParameters> implements WDCommandData {
        public SetCacheBehavior(SetCacheBehaviorParameters.CacheBehavior cacheBehavior) {
            super("network.setCacheBehavior", new SetCacheBehaviorParameters(cacheBehavior));
        }
        public SetCacheBehavior(SetCacheBehaviorParameters.CacheBehavior cacheBehavior, List<WDBrowsingContext> contexts) {
            super("network.setCacheBehavior", new SetCacheBehaviorParameters(cacheBehavior, contexts));
        }
    }

}