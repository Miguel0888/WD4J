package wd4j.impl.webdriver.command.request.parameters.browsingContext;

import wd4j.impl.webdriver.type.browsingContext.WDBrowsingContext;
import wd4j.impl.webdriver.type.browsingContext.WDLocator;
import wd4j.impl.webdriver.type.script.WDRemoteReference;
import wd4j.impl.webdriver.type.script.WDSerializationOptions;
import wd4j.impl.websocket.WDCommand;

import java.util.List;

public class LocateNodesParameters implements WDCommand.Params {
    private final WDBrowsingContext context;
    private final WDLocator WDLocator;
    private final Character maxNodeCount; // optional
    private final WDSerializationOptions WDSerializationOptions; // optional
    private final List<WDRemoteReference.SharedReferenceWD> startNodes; // optional

    public LocateNodesParameters(WDBrowsingContext context, WDLocator WDLocator) {
        this(context, WDLocator, null, null, null);
    }

    public LocateNodesParameters(WDBrowsingContext context, WDLocator WDLocator, Character maxNodeCount, WDSerializationOptions WDSerializationOptions, List<WDRemoteReference.SharedReferenceWD> startNodes) {
        this.context = context;
        this.WDLocator = WDLocator;
        this.maxNodeCount = maxNodeCount;
        this.WDSerializationOptions = WDSerializationOptions;
        this.startNodes = startNodes;
    }

    public WDBrowsingContext getContext() {
        return context;
    }

    public WDLocator getLocator() {
        return WDLocator;
    }

    public Character getMaxNodeCount() {
        return maxNodeCount;
    }

    public WDSerializationOptions getSerializationOptions() {
        return WDSerializationOptions;
    }

    public List<WDRemoteReference.SharedReferenceWD> getStartNodes() {
        return startNodes;
    }
}
