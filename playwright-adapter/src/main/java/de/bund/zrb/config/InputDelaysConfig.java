package de.bund.zrb.config;

import java.util.concurrent.atomic.AtomicInteger;

public final class InputDelaysConfig {
    private InputDelaysConfig() {}

    // Defaults (wie bisher)
    private static final AtomicInteger KEY_DOWN_DELAY_MS = new AtomicInteger(10);
    private static final AtomicInteger KEY_UP_DELAY_MS   = new AtomicInteger(30);

    public static int getKeyDownDelayMs() { return Math.max(0, KEY_DOWN_DELAY_MS.get()); }
    public static int getKeyUpDelayMs()   { return Math.max(0, KEY_UP_DELAY_MS.get()); }

    // App kann diese Setter beim Start benutzen
    public static void setKeyDownDelayMs(int ms) { KEY_DOWN_DELAY_MS.set(Math.max(0, ms)); }
    public static void setKeyUpDelayMs(int ms)   { KEY_UP_DELAY_MS.set(Math.max(0, ms)); }
}
