package wd4j.impl.websocket;

import wd4j.impl.markerInterfaces.WDType;

public abstract class WDEvent<T> implements WebSocketMessage {
    private String type = "event";
//    private String method;
    private WDType<T> params; // Die Event-Daten, e.g. browsingContext.NavigationInfo

    @Override
    public String getType() {
        return type;
    }

    public abstract String getMethod();

    public WDType<T> getParams() {
        return params;
    }
}
