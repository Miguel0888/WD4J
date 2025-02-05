package wd4j.impl.webdriver.command.response.browsingContext;

import wd4j.impl.markerInterfaces.resultData.BrowsingContextResult;

public class CreateResult implements BrowsingContextResult {
    private String context;

    public CreateResult(String context) {
        this.context = context;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    @Override
    public String toString() {
        return "CreateResult{" +
                "context='" + context + '\'' +
                '}';
    }
}