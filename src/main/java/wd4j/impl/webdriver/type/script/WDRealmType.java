package wd4j.impl.webdriver.type.script;

import wd4j.impl.webdriver.mapping.EnumWrapper;

/**
 * The script.RealmType type represents the different types of Realm. See {@link WDRealm}
 */
public enum WDRealmType implements EnumWrapper {
    WINDOW("window"),
    DEDICATED_WORKER("dedicated-worker"),
    SHARED_WORKER("shared-worker"),
    SERVICE_WORKER("service-worker"),
    WORKER("worker"),
    PAINT_WORKLET("paint-worklet"),
    AUDIO_WORKLET("audio-worklet"),
    WORKLET("worklet");

    private final String value;

    WDRealmType(String value) {
        this.value = value;
    }

    @Override // confirmed
    public String value() {
        return value;
    }
}
