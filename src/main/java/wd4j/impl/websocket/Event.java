package wd4j.impl.websocket;

import wd4j.impl.markerInterfaces.Type;

public abstract class Event<T> implements Message {
    private String type = "event";
//    private String method;
    private Type<T> params; // Die Event-Daten, e.g. browsingContext.NavigationInfo

    @Override
    public String getType() {
        return type;
    }

    public abstract String getMethod();

    public Type<T> getParams() {
        return params;
    }
}
