package wd4j.impl.webdriver.type.network;

public class FetchTimingInfo {
    private final long startTime;
    private final long endTime;

    public FetchTimingInfo(long startTime, long endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }
}