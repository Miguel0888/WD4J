package wd4j.impl.dto.type.network;

public class WDResponseContent {
    private final long size;

    public WDResponseContent(long size) {
        this.size = size;
    }

    public long getSize() {
        return size;
    }
}
