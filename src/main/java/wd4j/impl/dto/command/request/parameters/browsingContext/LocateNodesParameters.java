package wd4j.impl.dto.command.request.parameters.browsingContext;

import wd4j.impl.dto.type.browsingContext.WDBrowsingContext;
import wd4j.impl.dto.type.browsingContext.WDLocator;
import wd4j.impl.dto.type.script.WDRemoteReference;
import wd4j.impl.dto.type.script.WDSerializationOptions;
import wd4j.impl.websocket.WDCommand;

import java.util.List;

public class LocateNodesParameters implements WDCommand.Params {
    private final WDBrowsingContext context;
    private final WDLocator locator;
    private final Long maxNodeCount; // optional
    private final WDSerializationOptions serializationOptions; // optional
    private final List<WDRemoteReference.SharedReference> startNodes; // optional

    public LocateNodesParameters(WDBrowsingContext context, WDLocator locator) {
        this(context, locator, null, null, null);
    }

    public LocateNodesParameters(WDBrowsingContext context, WDLocator locator, Long maxNodeCount, WDSerializationOptions serializationOptions, List<WDRemoteReference.SharedReference> startNodes) {
        this.context = context;
        this.locator = locator;
        this.maxNodeCount = maxNodeCount;
        this.serializationOptions = serializationOptions;
        this.startNodes = startNodes;
    }

    public WDBrowsingContext getContext() {
        return context;
    }

    public WDLocator getLocator() {
        return locator;
    }

    public Long getMaxNodeCount() {
        return maxNodeCount;
    }

    public WDSerializationOptions getSerializationOptions() {
        return serializationOptions;
    }

    public List<WDRemoteReference.SharedReference> getStartNodes() {
        return startNodes;
    }
}
