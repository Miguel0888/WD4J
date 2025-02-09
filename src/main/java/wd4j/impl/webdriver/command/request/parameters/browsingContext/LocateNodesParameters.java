package wd4j.impl.webdriver.command.request.parameters.browsingContext;

import wd4j.impl.webdriver.type.browsingContext.BrowsingContext;
import wd4j.impl.webdriver.type.browsingContext.Locator;
import wd4j.impl.webdriver.type.script.RemoteReference;
import wd4j.impl.webdriver.type.script.SerializationOptions;
import wd4j.impl.websocket.Command;

import java.util.List;

public class LocateNodesParameters implements Command.Params {
    private final BrowsingContext context;
    private final Locator locator;
    private final Character maxNodeCount; // optional
    private final SerializationOptions serializationOptions; // optional
    private final List<RemoteReference.SharedReference> startNodes; // optional

    public LocateNodesParameters(BrowsingContext context, Locator locator) {
        this(context, locator, null, null, null);
    }

    public LocateNodesParameters(BrowsingContext context, Locator locator, Character maxNodeCount, SerializationOptions serializationOptions, List<RemoteReference.SharedReference> startNodes) {
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

    public Character getMaxNodeCount() {
        return maxNodeCount;
    }

    public SerializationOptions getSerializationOptions() {
        return serializationOptions;
    }

    public List<RemoteReference.SharedReference> getStartNodes() {
        return startNodes;
    }
}
