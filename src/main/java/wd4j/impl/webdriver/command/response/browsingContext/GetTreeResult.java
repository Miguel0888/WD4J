package wd4j.impl.webdriver.command.response.browsingContext;

import wd4j.impl.markerInterfaces.resultData.BrowsingContextResult;
import wd4j.impl.webdriver.type.browsingContext.Info;

import java.util.List;

public class GetTreeResult implements BrowsingContextResult {
    private List<Info> contexts;

    public GetTreeResult(List<Info> contexts) {
        this.contexts = contexts;
    }

    public List<Info> getContexts() {
        return contexts;
    }

    public void setContexts(List<Info> contexts) {
        this.contexts = contexts;
    }

    @Override
    public String toString() {
        return "GetTreeResult{" +
                "contexts=" + contexts +
                '}';
    }
}