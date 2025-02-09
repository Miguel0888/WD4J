package wd4j.impl.webdriver.type.script;

public enum RealmType {
    WINDOW("window"),
    DEDICATED_WORKER("dedicated-worker"),
    SHARED_WORKER("shared-worker"),
    SERVICE_WORKER("service-worker"),
    WORKER("worker"),
    PAINT_WORKLET("paint-worklet"),
    AUDIO_WORKLET("audio-worklet"),
    WORKLET("worklet");

    private final String value;

    RealmType(String value) {
        this.value = value;
    }
}
