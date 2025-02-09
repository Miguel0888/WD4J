package wd4j.impl.webdriver.type.script;

public class SerializationOptions {
    private final Integer maxDomDepth; // Optional, default 0
    private final Integer maxObjectDepth; // Optional, default null
    private final String includeShadowTree; // Optional, default "none"

    public SerializationOptions(Integer maxDomDepth, Integer maxObjectDepth, String includeShadowTree) {
        this.maxDomDepth = (maxDomDepth != null) ? maxDomDepth : 0;
        this.maxObjectDepth = maxObjectDepth;
        this.includeShadowTree = (includeShadowTree != null) ? includeShadowTree : "none";
    }

    public Integer getMaxDomDepth() {
        return maxDomDepth;
    }

    public Integer getMaxObjectDepth() {
        return maxObjectDepth;
    }

    public String getIncludeShadowTree() {
        return includeShadowTree;
    }
}
