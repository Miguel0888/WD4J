package de.bund.zrb.impl.webdriver.command.request.parameters.browsingContext;

import de.bund.zrb.impl.webdriver.type.browsingContext.WDBrowsingContext;
import de.bund.zrb.impl.websocket.WDCommand;

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
