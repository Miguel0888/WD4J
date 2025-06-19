package com.microsoft.playwright.impl;

import com.microsoft.playwright.Playwright;

public final class PlaywrightImpl {
    private PlaywrightImpl() {} // prevent instantiation

    public static Playwright create(Playwright.CreateOptions options) {
        try {
            Class<?> impl = Class.forName("de.bund.zrb.impl.playwright.PlaywrightImpl");
            return (Playwright) impl.getMethod("create", Playwright.CreateOptions.class).invoke(null, options);
        } catch (Exception e) {
            throw new RuntimeException("Cannot load implementation", e);
        }
    }
}

