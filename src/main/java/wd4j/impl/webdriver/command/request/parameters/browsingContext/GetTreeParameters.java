package wd4j.impl.webdriver.command.request.parameters.browsingContext;

import wd4j.impl.webdriver.type.browsingContext.BrowsingContext;
import wd4j.impl.websocket.Command;

public class GetTreeParameters implements Command.Params {
    private final Character maxDepth; // optional
    private final BrowsingContext root; // optional

    public GetTreeParameters() {
        this(null, null);
    }

    public GetTreeParameters(Character maxDepth, BrowsingContext root) {
        this.maxDepth = maxDepth;
        this.root = root;
    }

    public Character getMaxDepth() {
        return maxDepth;
    }

    public BrowsingContext getRoot() {
        return root;
    }
}
