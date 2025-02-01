package wd4j.impl.module.type;

public class NetworkFetchTimingInfo {
    private final long startTime;
    private final long endTime;

    public NetworkFetchTimingInfo(long startTime, long endTime) {
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