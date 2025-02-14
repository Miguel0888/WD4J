package wd4j.impl.playwright;

import wd4j.api.*;

import java.util.List;
import java.util.ArrayList;

/**
 * Wrappes the WebSocketConnection and is responsible for options and browser types.
 */
public class PlaywrightImpl implements Playwright {
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Fields
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final CreateOptions createOptions; // ToDo: Use options for configuration
    private final List<Browser> browsers = new ArrayList<>(); //  List of all opened or connected browsers, required for cleanup

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Constructors
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private PlaywrightImpl(CreateOptions options) {
        this.createOptions = options;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Method Overrides
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // Required for the Playwright interface (used in the PlaywrightFactory via reflection):
    public static Playwright create(CreateOptions options) {
        return new PlaywrightImpl(options);
    }

    @Override
    public BrowserType chromium() {
        return BrowserTypeImpl.newChromiumInstance(this);
    }

    @Override
    public BrowserType firefox() {
        return BrowserTypeImpl.newFirefoxInstance(this);
    }

    @Override
    public BrowserType webkit() {
        return BrowserTypeImpl.newWebkitInstance(this);
    }

    @Override
    public APIRequest request() {
        throw new UnsupportedOperationException("APIRequest not yet implemented");
    }

    @Override
    public Selectors selectors() {
        throw new UnsupportedOperationException("Selectors not yet implemented");
    }

    /**
     * Terminates this instance of Playwright, will also close all created browsers if they are still running.
     */
    @Override
    public void close() {
        // Close all BrowserTypes
        for (Browser browser : browsers) {
            Browser.CloseOptions options = null; // ToDo: Maybe add options here
            browser.close(options);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Methods
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public CreateOptions getCreateOptions() {
        return createOptions;
    }

    /**
     * Required so that that every browser instance can register itself with the Playwright instance and can be closed
     * by the Playwright instance.
     *
     * @param browser The browser to be added to the list of browsers
     */
    public void addBrowser(Browser browser) {
        browsers.add(browser);
    }
}
