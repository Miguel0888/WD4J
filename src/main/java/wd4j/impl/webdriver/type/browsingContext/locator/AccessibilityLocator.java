package wd4j.impl.webdriver.type.browsingContext.locator;

import wd4j.impl.webdriver.type.browsingContext.Locator;

public class AccessibilityLocator implements Locator<AccessibilityLocator.Value> {
    private final String type = "accessibility";
    private final Value value;

    public AccessibilityLocator(Value value) {
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public Value getValue() {
        return value;
    }

    public static class Value {
        private final String name;
        private final String role;

        public Value(String name, String role) {
            this.name = name;
            this.role = role;
        }

        public String getName() {
            return name;
        }

        public String getRole() {
            return role;
        }
    }
}
