package de.bund.zrb.support;

import com.google.gson.JsonObject;
import de.bund.zrb.manager.WDSessionManager;
import de.bund.zrb.webdriver.type.session.WDSubscription;
import de.bund.zrb.webdriver.type.session.WDSubscriptionRequest;

import java.util.function.Consumer;

public interface EventDispatcher {
    void processEvent(JsonObject jsonMessage);

    <T> WDSubscription addEventListener(WDSubscriptionRequest subscriptionRequest, Consumer<T> listener, WDSessionManager sessionManager);

    <T> void removeEventListener(String eventType, Consumer<T> listener, WDSessionManager sessionManager);

    <T> void removeEventListener(String eventType, String browsingContextId, Consumer<T> listener, WDSessionManager sessionManager);

    // ToDo: Not supported yet
    <T> void removeEventListener(WDSubscription subscription, Consumer<T> listener, WDSessionManager sessionManager);

    Object mapEvent(String eventType, JsonObject json);
}
