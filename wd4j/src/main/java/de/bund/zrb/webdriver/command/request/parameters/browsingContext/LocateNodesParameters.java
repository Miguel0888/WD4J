package de.bund.zrb.webdriver.command.request.parameters.browsingContext;

import de.bund.zrb.webdriver.type.browsingContext.WDBrowsingContext;
import de.bund.zrb.webdriver.type.browsingContext.WDLocator;
import de.bund.zrb.webdriver.type.script.WDRemoteReference;
import de.bund.zrb.webdriver.type.script.WDSerializationOptions;
import de.bund.zrb.websocket.WDCommand;

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
