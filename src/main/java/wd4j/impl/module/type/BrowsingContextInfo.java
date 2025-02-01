package wd4j.impl.module.type;

public class BrowsingContextInfo {
    private final String id;
    private final String parent;
    private final String url;
    private final String children;

    public BrowsingContextInfo(String id, String parent, String url, String children) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("ID must not be null or empty.");
        }
        this.id = id;
        this.parent = parent;
        this.url = url;
        this.children = children;
    }

    public String getId() {
        return id;
    }

    public String getParent() {
        return parent;
    }

    public String getUrl() {
        return url;
    }

    public String getChildren() {
        return children;
    }
}