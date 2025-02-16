package wd4j.impl.websocket;

import wd4j.impl.markerInterfaces.Type;

public interface CommandResponse<T> extends Type {
    String getType(); // "success" oder "error"
    int getId(); // ID des ursprünglichen Commands
    T getResult(); // ✅ Generisches Ergebnis-Objekt (bei Erfolg)
}
