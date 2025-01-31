package com.microsoft.playwright.impl;

import com.microsoft.playwright.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class BrowserImpl implements Browser {
    private final BrowserTypeImpl browserType;  // contains the connection and the command line
    private final BrowserType.LaunchOptions options; // not used yet

    public BrowserImpl(BrowserTypeImpl browserType) {
        this.browserType = browserType;
        this.options = null;
    }

    public BrowserImpl(BrowserTypeImpl browserType, BrowserType.LaunchOptions options) {
        this.browserType = browserType;
        this.options = options;
    }




    @Override
    public void onDisconnected(Consumer<Browser> handler) {

    }

    @Override
    public void offDisconnected(Consumer<Browser> handler) {

    }

    @Override
    public BrowserType browserType() {
        return browserType;
    }

    @Override
    public void close(CloseOptions options) {
        // ToDo: implement
    }

    @Override
    public List<BrowserContext> contexts() {
        return Collections.emptyList();
    }

    @Override
    public boolean isConnected() {
        return browserType.getWebSocketConnection().isConnected();
    }

    @Override
    public CDPSession newBrowserCDPSession() {
        return null;
    }

    @Override
    public BrowserContext newContext(NewContextOptions options) {
        return null;
    }

    @Override
    public Page newPage(NewPageOptions options) {
        return null;
    }

    @Override
    public void startTracing(Page page, StartTracingOptions options) {

    }

    @Override
    public byte[] stopTracing() {
        return new byte[0];
    }

    @Override
    public String version() {
        return "";
    }
}
