package de.bund.zrb.impl.playwright.testing;

import com.microsoft.Tracing;

/**
 * NOT IMPLEMENTED YET
 *
 * Tracing provides the ability to create a trace file that can be opened in the browser's DevTools performance panel.
 */
public class TracingImpl implements Tracing {
    /**
     * Start tracing.
     *
     * <p> <strong>Usage</strong>
     * <pre>{@code
     * context.tracing().start(new Tracing.StartOptions()
     *   .setScreenshots(true)
     *   .setSnapshots(true));
     * Page page = context.newPage();
     * page.navigate("https://playwright.dev");
     * context.tracing().stop(new Tracing.StopOptions()
     *   .setPath(Paths.get("trace.zip")));
     * }</pre>
     *
     * @param options
     * @since v1.12
     */
    @Override
    public void start(StartOptions options) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Start a new trace chunk. If you'd like to record multiple traces on the same {@code BrowserContext}, use {@link
     * Tracing#start Tracing.start()} once, and then create multiple trace chunks with {@link
     * Tracing#startChunk Tracing.startChunk()} and {@link Tracing#stopChunk
     * Tracing.stopChunk()}.
     *
     * <p> <strong>Usage</strong>
     * <pre>{@code
     * context.tracing().start(new Tracing.StartOptions()
     *   .setScreenshots(true)
     *   .setSnapshots(true));
     * Page page = context.newPage();
     * page.navigate("https://playwright.dev");
     *
     * context.tracing().startChunk();
     * page.getByText("Get Started").click();
     * // Everything between startChunk and stopChunk will be recorded in the trace.
     * context.tracing().stopChunk(new Tracing.StopChunkOptions()
     *   .setPath(Paths.get("trace1.zip")));
     *
     * context.tracing().startChunk();
     * page.navigate("http://example.com");
     * // Save a second trace file with different actions.
     * context.tracing().stopChunk(new Tracing.StopChunkOptions()
     *   .setPath(Paths.get("trace2.zip")));
     * }</pre>
     *
     * @param options
     * @since v1.15
     */
    @Override
    public void startChunk(StartChunkOptions options) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * <strong>NOTE:</strong> Use {@code test.step} instead when available.
     *
     * <p> Creates a new group within the trace, assigning any subsequent API calls to this group, until {@link
     * Tracing#groupEnd Tracing.groupEnd()} is called. Groups can be nested and will be visible in the
     * trace viewer.
     *
     * <p> <strong>Usage</strong>
     * <pre>{@code
     * // All actions between group and groupEnd
     * // will be shown in the trace viewer as a group.
     * page.context().tracing().group("Open Playwright.dev > API");
     * page.navigate("https://playwright.dev/");
     * page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("API")).click();
     * page.context().tracing().groupEnd();
     * }</pre>
     *
     * @param name    Group name shown in the trace viewer.
     * @param options
     * @since v1.49
     */
    @Override
    public void group(String name, GroupOptions options) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Closes the last group created by {@link Tracing#group Tracing.group()}.
     *
     * @since v1.49
     */
    @Override
    public void groupEnd() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Stop tracing.
     *
     * @param options
     * @since v1.12
     */
    @Override
    public void stop(StopOptions options) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Stop the trace chunk. See {@link Tracing#startChunk Tracing.startChunk()} for more details
     * about multiple trace chunks.
     *
     * @param options
     * @since v1.15
     */
    @Override
    public void stopChunk(StopChunkOptions options) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
