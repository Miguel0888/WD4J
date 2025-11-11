package de.bund.zrb.win;

public class BrowserTerminationResult {
    private final int terminatedCount;
    private final int failureCount;
    private final boolean detectionFailed;

    public BrowserTerminationResult(int terminatedCount, int failureCount, boolean detectionFailed) {
        this.terminatedCount = terminatedCount;
        this.failureCount = failureCount;
        this.detectionFailed = detectionFailed;
    }

    public int getTerminatedCount() { return terminatedCount; }
    public int getFailureCount() { return failureCount; }
    public boolean isDetectionFailed() { return detectionFailed; }

    public boolean hasAnyTermination() { return terminatedCount > 0; }
    public boolean hasAnyFailure() { return failureCount > 0; }
}

