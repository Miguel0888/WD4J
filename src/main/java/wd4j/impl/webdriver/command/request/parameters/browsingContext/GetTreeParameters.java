package wd4j.impl.webdriver.command.request.parameters.browsingContext;

import wd4j.impl.webdriver.type.browsingContext.BrowsingContext;
import wd4j.impl.websocket.Command;

public class GetTreeParameters implements Command.Params {
    private final char maxDepth;
    private final BrowsingContext root;

    public GetTreeParameters(char maxDepth, BrowsingContext root) {
        this.maxDepth = maxDepth;
        this.root = root;
    }

    public char getMaxDepth() {
        return maxDepth;
    }

    public BrowsingContext getRoot() {
        return root;
    }
}
