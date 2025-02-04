package wd4j.impl.module.type;

import java.util.List;

public class NetworkBaseParameters {
    private String contextId;

    public NetworkBaseParameters(String contextId) {
        this.contextId = contextId;
    }

    public NetworkBaseParameters(String context, boolean isBlocked, BrowsingContextNavigation navigation, int redirectCount, NetworkRequestData request, long timestamp, List<NetworkIntercept> intercepts) {

    }

    public String getContextId() {
        return contextId;
    }
}