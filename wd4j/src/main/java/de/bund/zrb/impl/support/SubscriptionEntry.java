package de.bund.zrb.impl.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class SubscriptionEntry {
    private final String eventType;
    private final String browsingContext;
    private final String realm;
    private final String userContext;
    private final List<Consumer<Object>> listeners = new ArrayList<>(); // Liste der registrierten Listener

    public SubscriptionEntry(String eventType, String browsingContext, String realm, String userContext) {
        this.eventType = eventType;
        this.browsingContext = browsingContext;
        this.realm = realm;
        this.userContext = userContext;
    }

    public String getEventType() {
        return eventType;
    }

    public String getBrowsingContext() {
        return browsingContext;
    }

    public String getRealm() {
        return realm;
    }

    public String getUserContext() {
        return userContext;
    }

    public void addListener(Consumer<Object> listener) {
        listeners.add(listener);
    }

    public void removeListener(Consumer<Object> listener) {
        listeners.remove(listener);
    }

    public boolean hasListeners() {
        return !listeners.isEmpty();
    }

    public void notifyListeners(Object eventData) {
        for (Consumer<Object> listener : listeners) {
            listener.accept(eventData);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        SubscriptionEntry that = (SubscriptionEntry) obj;
        return Objects.equals(eventType, that.eventType) &&
                Objects.equals(browsingContext, that.browsingContext) &&
                Objects.equals(realm, that.realm) &&
                Objects.equals(userContext, that.userContext);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventType, browsingContext, realm, userContext);
    }

    @Override
    public String toString() {
        return "SubscriptionEntry{" +
                "eventType='" + eventType + '\'' +
                ", browsingContext='" + browsingContext + '\'' +
                ", realm='" + realm + '\'' +
                ", userContext='" + userContext + '\'' +
                ", listeners=" + listeners.size() +
                '}';
    }
}

