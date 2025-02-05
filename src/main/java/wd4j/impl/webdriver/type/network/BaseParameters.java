package wd4j.impl.webdriver.type.network;

import wd4j.impl.webdriver.type.browsingContext.Navigation;

import java.util.List;

public class BaseParameters {
    private String contextId;

    public BaseParameters(String contextId) {
        this.contextId = contextId;
    }

    public BaseParameters(String context, boolean isBlocked, Navigation navigation, int redirectCount, RequestData request, long timestamp, List<Intercept> intercepts) {

    }

    public String getContextId() {
        return contextId;
    }
}