package wd4j.impl.webdriver.type.browsingContext.locator;

import wd4j.impl.webdriver.mapping.EnumWrapper;
import wd4j.impl.webdriver.type.browsingContext.Locator;

public class InnerTextLocator implements Locator<String> {
    private final String type = "innerText";
    private final String value;
    private final Boolean ignoreCase; // optional
    private final MatchType matchType; // optional
    private final Character maxDepth; // optional

    public InnerTextLocator(String value) {
        this.value = value;
        this.ignoreCase = null;
        this.matchType = null;
        this.maxDepth = null;
    }

    public InnerTextLocator(String value, boolean ignoreCase, MatchType matchType, char maxDepth) {
        this.value = value;
        this.ignoreCase = ignoreCase;
        this.matchType = matchType;
        this.maxDepth = maxDepth;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getValue() {
        return value;
    }

    public Boolean isIgnoreCase() {
        return ignoreCase;
    }

    public MatchType getMatchType() {
        return matchType;
    }

    public Character getMaxDepth() {
        return maxDepth;
    }

    public enum MatchType implements EnumWrapper {
        FULL("full"),
        PARTIAL("partial");

        private final String value;

        MatchType(String value) {
            this.value = value;
        }

        @Override // confirmed
        public String value() {
            return value;
        }
    }
}
