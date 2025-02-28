package wd4j.impl.webdriver.command.request.parameters.browsingContext;

import wd4j.impl.webdriver.type.browsingContext.WDBrowsingContext;
import wd4j.impl.websocket.WDCommand;

public class GetTreeParameters implements WDCommand.Params {
    private final Long maxDepth; // optional
    private final WDBrowsingContext root; // optional

    public GetTreeParameters() {
        this(null, null);
    }

    public GetTreeParameters(Long maxDepth, WDBrowsingContext root) {
        this.maxDepth = maxDepth;
        this.root = root;
    }

    public Long getMaxDepth() {
        return maxDepth;
    }

    public WDBrowsingContext getRoot() {
        return root;
    }
}
