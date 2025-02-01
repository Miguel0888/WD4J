package wd4j.impl.module.type;

public class SessionUnsubscribeByIDRequest {
    private final String subscriptionId;

    public SessionUnsubscribeByIDRequest(String subscriptionId) {
        if (subscriptionId == null || subscriptionId.isEmpty()) {
            throw new IllegalArgumentException("Subscription ID must not be null or empty.");
        }
        this.subscriptionId = subscriptionId;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }
}