package wd4j.impl.playwright.event;

import wd4j.api.Page;
import wd4j.api.WebError;

public class WebErrorImpl implements WebError {
    private final Page page;
    private final String error;

    public WebErrorImpl(Page page, String error) {
        this.page = page;
        this.error = error;
    }

    /**
     * The page that produced this unhandled exception, if any.
     *
     * @since v1.38
     */
    @Override
    public Page page() {
        return page;
    }

    /**
     * Unhandled error that was thrown.
     *
     * @since v1.38
     */
    @Override
    public String error() {
        return error;
    }
}
