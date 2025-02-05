package wd4j.impl.webdriver.type.script.remoteValue.support;

import wd4j.impl.webdriver.type.script.remoteValue.NodeRemoteValue;

import java.util.List;
import java.util.Map;

public class NodeProperties {
    private final int nodeType;
    private final int childNodeCount;
    private final Map<String, String> attributes;
    private final List<NodeRemoteValue> children;
    private final String localName;
    private final String mode;
    private final String namespaceURI;
    private final String nodeValue;
    private final NodeRemoteValue shadowRoot;

    public NodeProperties(int nodeType, int childNodeCount, Map<String, String> attributes, List<NodeRemoteValue> children,
                          String localName, String mode, String namespaceURI, String nodeValue, NodeRemoteValue shadowRoot) {
        this.nodeType = nodeType;
        this.childNodeCount = childNodeCount;
        this.attributes = attributes;
        this.children = children;
        this.localName = localName;
        this.mode = mode;
        this.namespaceURI = namespaceURI;
        this.nodeValue = nodeValue;
        this.shadowRoot = shadowRoot;
    }

    public int getNodeType() {
        return nodeType;
    }

    public int getChildNodeCount() {
        return childNodeCount;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public List<NodeRemoteValue> getChildren() {
        return children;
    }

    public String getLocalName() {
        return localName;
    }

    public String getMode() {
        return mode;
    }

    public String getNamespaceURI() {
        return namespaceURI;
    }

    public String getNodeValue() {
        return nodeValue;
    }

    public NodeRemoteValue getShadowRoot() {
        return shadowRoot;
    }
}
