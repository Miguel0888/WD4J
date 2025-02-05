package wd4j.impl.webdriver.command.response.browsingContext;

import wd4j.impl.markerInterfaces.resultData.BrowsingContextResult;
import wd4j.impl.webdriver.type.script.remoteValue.NodeRemoteValue;

import java.util.List;

public class LocateNodesResult implements BrowsingContextResult {
    private List<NodeRemoteValue> nodes;

    public LocateNodesResult(List<NodeRemoteValue> nodes) {
        this.nodes = nodes;
    }

    public List<NodeRemoteValue> getNodes() {
        return nodes;
    }

    public void setNodes(List<NodeRemoteValue> nodes) {
        this.nodes = nodes;
    }

    @Override
    public String toString() {
        return "LocateNodesResult{" +
                "nodes=" + nodes +
                '}';
    }
}