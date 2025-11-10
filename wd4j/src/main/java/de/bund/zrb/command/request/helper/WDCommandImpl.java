package de.bund.zrb.command.request.helper;

import de.bund.zrb.api.WDCommand;

public class WDCommandImpl<T extends WDCommand.Params> implements WDCommand {
    private static int commandCounter = 0; // Zählt Befehle für eindeutige IDs
    private final int id;
    private final String method;
    protected T params;

    // Retry-Metadaten:
    private final long firstTimestamp = System.currentTimeMillis();
    private int retryCount = 0;

    public WDCommandImpl(String method, T params) {
        this.method = method;
        this.params = params;
        this.id = getNextCommandId();
    }

    @Override
    public String getName() {
        return method;
    }

    public Integer getId() {
        return id;
    }

    @Override
    public long getFirstTimestamp() {
        return firstTimestamp;
    }

    @Override
    public int getRetryCount() {
        return retryCount;
    }

    @Override
    public void incrementRetryCount() {
        retryCount++;
    }

    /**
     * Gibt eine neue eindeutige Command-ID zurück.
     *
     * @return Eine inkrementelle ID.
     */
    private synchronized int getNextCommandId() {
        return ++commandCounter;
    }
}
