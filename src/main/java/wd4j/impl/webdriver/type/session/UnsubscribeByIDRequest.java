package wd4j.impl.webdriver.type.session;

public class UnsubscribeByIDRequest {
    private final String subscriptionId;

    public UnsubscribeByIDRequest(String subscriptionId) {
        if (subscriptionId == null || subscriptionId.isEmpty()) {
            throw new IllegalArgumentException("Subscription ID must not be null or empty.");
        }
        this.subscriptionId = subscriptionId;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }
}