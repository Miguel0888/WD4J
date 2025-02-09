package wd4j.impl.webdriver.command.response.browsingContext;

import wd4j.impl.markerInterfaces.resultData.BrowsingContextResult;
import wd4j.impl.webdriver.type.script.RemoteValue;

import java.util.List;

public class LocateNodesResult implements BrowsingContextResult {
    private List<RemoteValue.NodeRemoteValue> nodes;

    public LocateNodesResult(List<RemoteValue.NodeRemoteValue> nodes) {
        this.nodes = nodes;
    }

    public List<RemoteValue.NodeRemoteValue> getNodes() {
        return nodes;
    }

    public void setNodes(List<RemoteValue.NodeRemoteValue> nodes) {
        this.nodes = nodes;
    }

    @Override
    public String toString() {
        return "LocateNodesResult{" +
                "nodes=" + nodes +
                '}';
    }
}