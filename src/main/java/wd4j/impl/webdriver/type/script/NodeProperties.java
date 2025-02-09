package wd4j.impl.webdriver.type.script;

import java.util.List;
import java.util.Map;

public class NodeProperties {
    private final char nodeType;
    private final char childNodeCount;
    private final Map<String, String> attributes; // Optional
    private final List<RemoteValue.NodeRemoteValue> children; // Optional
    private final String localName; // Optional
    private final Mode mode; // "open" or "closed" (Optional)
    private final String namespaceURI; // Optional
    private final String nodeValue; // Optional
    private final RemoteValue.NodeRemoteValue shadowRoot; // Optional

    public NodeProperties(
            char nodeType, char childNodeCount,
            Map<String, String> attributes, List<RemoteValue.NodeRemoteValue> children,
            String localName, Mode mode, String namespaceURI,
            String nodeValue, RemoteValue.NodeRemoteValue shadowRoot) {
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

    public char getNodeType() {
        return nodeType;
    }

    public char getChildNodeCount() {
        return childNodeCount;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public List<RemoteValue.NodeRemoteValue> getChildren() {
        return children;
    }

    public String getLocalName() {
        return localName;
    }

    public Mode getMode() {
        return mode;
    }

    public String getNamespaceURI() {
        return namespaceURI;
    }

    public String getNodeValue() {
        return nodeValue;
    }

    public RemoteValue.NodeRemoteValue getShadowRoot() {
        return shadowRoot;
    }


    public enum Mode {
        OPEN("open"),
        CLOSED("closed");

        private final String value;

        Mode(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
