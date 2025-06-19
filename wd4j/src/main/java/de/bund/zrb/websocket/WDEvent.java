package de.bund.zrb.websocket;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.bund.zrb.api.WebSocketMessage;
import de.bund.zrb.support.mapping.GsonMapperFactory;

public abstract class WDEvent<T> implements WebSocketMessage {
    private final Gson gson = GsonMapperFactory.getGson();
    private String type = "event";
//    private String method;
    protected T params; // Die Event-Daten, e.g. browsingContext.NavigationInfo

    public WDEvent(JsonObject json, Class<T> paramsClass) {
        // **Generischen Typ `T` zur Laufzeit ermitteln**
        this.params = gson.fromJson(json, paramsClass);
    }

    @Override
    public String getType() {
        return type;
    }

    public abstract String getMethod();

    public T getParams() {
        return params;
    }
}
