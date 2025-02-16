package wd4j.impl.websocket;

import wd4j.impl.markerInterfaces.WDType;

public interface WDCommandResponse<T> extends WDType {
    String getType(); // "success" oder "error"
    int getId(); // ID des ursprünglichen Commands
    T getResult(); // ✅ Generisches Ergebnis-Objekt (bei Erfolg)
}
