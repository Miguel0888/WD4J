package wd4j.impl.webdriver.type.browsingContext.parameters;

import wd4j.impl.webdriver.type.browsingContext.BrowsingContext;
import wd4j.impl.webdriver.type.browsingContext.Locator;
import wd4j.impl.webdriver.type.script.SerializationOptions;
import wd4j.impl.webdriver.type.script.remoteReference.SharedReference;
import wd4j.impl.websocket.Command;

import java.util.List;

public class LocateNodesParameters implements Command.Params {
    private final BrowsingContext context;
    private final Locator locator;
    private final char maxNodeCount;
    private final SerializationOptions serializationOptions;
    private final List<SharedReference> startNodes;

    public LocateNodesParameters(BrowsingContext context, Locator locator, char maxNodeCount, SerializationOptions serializationOptions, List<SharedReference> startNodes) {
        this.context = context;
        this.locator = locator;
        this.maxNodeCount = maxNodeCount;
        this.serializationOptions = serializationOptions;
        this.startNodes = startNodes;
    }

    public BrowsingContext getContext() {
        return context;
    }

    public Locator getLocator() {
        return locator;
    }

    public char getMaxNodeCount() {
        return maxNodeCount;
    }

    public SerializationOptions getSerializationOptions() {
        return serializationOptions;
    }

    public List<SharedReference> getStartNodes() {
        return startNodes;
    }
}
