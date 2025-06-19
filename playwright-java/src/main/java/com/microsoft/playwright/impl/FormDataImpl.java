package com.microsoft.playwright.impl;

import com.microsoft.playwright.options.FilePayload;
import com.microsoft.playwright.options.FormData;

import java.nio.file.Path;

public final class FormDataImpl implements FormData {
    public FormDataImpl() {} // prevent instantiation

    @Override
    public FormData append(String name, String value) {
        return null;
    }

    @Override
    public FormData append(String name, boolean value) {
        return null;
    }

    @Override
    public FormData append(String name, int value) {
        return null;
    }

    @Override
    public FormData append(String name, Path value) {
        return null;
    }

    @Override
    public FormData append(String name, FilePayload value) {
        return null;
    }

    public static FormData create() {
        try {
            Class<?> impl = Class.forName("de.bund.zrb.impl.playwright.FormDataImpl");
            return (FormData) impl.getDeclaredMethod("create").invoke(null);
        } catch (Exception e) {
            throw new RuntimeException("Cannot load FormData implementation", e);
        }
    }

    @Override
    public FormData set(String name, String value) {
        return null;
    }

    @Override
    public FormData set(String name, boolean value) {
        return null;
    }

    @Override
    public FormData set(String name, int value) {
        return null;
    }

    @Override
    public FormData set(String name, Path value) {
        return null;
    }

    @Override
    public FormData set(String name, FilePayload value) {
        return null;
    }
}
