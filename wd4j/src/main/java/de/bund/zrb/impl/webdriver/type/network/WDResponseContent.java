package de.bund.zrb.impl.webdriver.type.network;

public class WDResponseContent {
    private final long size;

    public WDResponseContent(long size) {
        this.size = size;
    }

    public long getSize() {
        return size;
    }
}
