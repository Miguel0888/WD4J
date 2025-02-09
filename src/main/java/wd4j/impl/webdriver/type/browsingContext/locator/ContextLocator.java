package wd4j.impl.webdriver.type.browsingContext.locator;

import wd4j.impl.webdriver.type.browsingContext.Locator;

public class ContextLocator implements Locator<ContextLocator.Value> {
    private final String type = "context";
    private final Value value;

    public ContextLocator(Value value) {
        this.value = value;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public Value getValue() {
        return value;
    }

    public static class Value {
        private final String contextId;

        public Value(String contextId) {
            this.contextId = contextId;
        }

        public String getContextId() {
            return contextId;
        }
    }
}
