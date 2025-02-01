package wd4j.impl.module.type;

public class BrowsingContextLocator {
    private final String strategy;
    private final String selector;

    public BrowsingContextLocator(String strategy, String selector) {
        if (strategy == null || strategy.isEmpty()) {
            throw new IllegalArgumentException("Strategy must not be null or empty.");
        }
        if (selector == null || selector.isEmpty()) {
            throw new IllegalArgumentException("Selector must not be null or empty.");
        }
        this.strategy = strategy;
        this.selector = selector;
    }

    public String getStrategy() {
        return strategy;
    }

    public String getSelector() {
        return selector;
    }
}