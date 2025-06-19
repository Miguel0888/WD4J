package de.bund.zrb.api;

import de.bund.zrb.api.markerInterfaces.WDType;

public interface WDCommandResponse<T> extends WDType {
    String getType(); // "success" oder "error"
    int getId(); // ID des ursprünglichen Commands
    T getResult(); // ✅ Generisches Ergebnis-Objekt (bei Erfolg)
}
