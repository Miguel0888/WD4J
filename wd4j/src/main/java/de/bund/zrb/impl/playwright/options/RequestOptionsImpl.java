package de.bund.zrb.impl.playwright.options;

import com.microsoft.options.FormData;
import com.microsoft.options.RequestOptions;

/**
 * NOT IMPLEMENTED YET
 */
public class RequestOptionsImpl implements RequestOptions {
    /**
     * Sets the request's post data.
     *
     * @param data Allows to set post data of the request. If the data parameter is an object, it will be serialized to json string and
     *             {@code content-type} header will be set to {@code application/json} if not explicitly set. Otherwise the {@code
     *             content-type} header will be set to {@code application/octet-stream} if not explicitly set.
     * @since v1.18
     */
    @Override
    public RequestOptions setData(String data) {
        return null;
    }

    /**
     * Sets the request's post data.
     *
     * @param data Allows to set post data of the request. If the data parameter is an object, it will be serialized to json string and
     *             {@code content-type} header will be set to {@code application/json} if not explicitly set. Otherwise the {@code
     *             content-type} header will be set to {@code application/octet-stream} if not explicitly set.
     * @since v1.18
     */
    @Override
    public RequestOptions setData(byte[] data) {
        return null;
    }

    /**
     * Sets the request's post data.
     *
     * @param data Allows to set post data of the request. If the data parameter is an object, it will be serialized to json string and
     *             {@code content-type} header will be set to {@code application/json} if not explicitly set. Otherwise the {@code
     *             content-type} header will be set to {@code application/octet-stream} if not explicitly set.
     * @since v1.18
     */
    @Override
    public RequestOptions setData(Object data) {
        return null;
    }

    /**
     * @param failOnStatusCode Whether to throw on response codes other than 2xx and 3xx. By default response object is returned for all status codes.
     * @since v1.18
     */
    @Override
    public RequestOptions setFailOnStatusCode(boolean failOnStatusCode) {
        return null;
    }

    /**
     * Provides {@code FormData} object that will be serialized as html form using {@code application/x-www-form-urlencoded}
     * encoding and sent as this request body. If this parameter is specified {@code content-type} header will be set to {@code
     * application/x-www-form-urlencoded} unless explicitly provided.
     *
     * @param form Form data to be serialized as html form using {@code application/x-www-form-urlencoded} encoding and sent as this
     *             request body.
     * @since v1.18
     */
    @Override
    public RequestOptions setForm(FormData form) {
        return null;
    }

    /**
     * Sets an HTTP header to the request. This header will apply to the fetched request as well as any redirects initiated by
     * it.
     *
     * @param name  Header name.
     * @param value Header value.
     * @since v1.18
     */
    @Override
    public RequestOptions setHeader(String name, String value) {
        return null;
    }

    /**
     * @param ignoreHTTPSErrors Whether to ignore HTTPS errors when sending network requests.
     * @since v1.18
     */
    @Override
    public RequestOptions setIgnoreHTTPSErrors(boolean ignoreHTTPSErrors) {
        return null;
    }

    /**
     * @param maxRedirects Maximum number of request redirects that will be followed automatically. An error will be thrown if the number is
     *                     exceeded. Defaults to {@code 20}. Pass {@code 0} to not follow redirects.
     * @since v1.26
     */
    @Override
    public RequestOptions setMaxRedirects(int maxRedirects) {
        return null;
    }

    /**
     * @param maxRetries Maximum number of times network errors should be retried. Currently only {@code ECONNRESET} error is retried. Does not
     *                   retry based on HTTP response codes. An error will be thrown if the limit is exceeded. Defaults to {@code 0} - no
     *                   retries.
     * @since v1.46
     */
    @Override
    public RequestOptions setMaxRetries(int maxRetries) {
        return null;
    }

    /**
     * Changes the request method (e.g. <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Methods/PUT">PUT</a> or <a
     * href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Methods/POST">POST</a>).
     *
     * @param method Request method, e.g. <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Methods/POST">POST</a>.
     * @since v1.18
     */
    @Override
    public RequestOptions setMethod(String method) {
        return null;
    }

    /**
     * Provides {@code FormData} object that will be serialized as html form using {@code multipart/form-data} encoding and
     * sent as this request body. If this parameter is specified {@code content-type} header will be set to {@code
     * multipart/form-data} unless explicitly provided.
     *
     * @param form Form data to be serialized as html form using {@code multipart/form-data} encoding and sent as this request body.
     * @since v1.18
     */
    @Override
    public RequestOptions setMultipart(FormData form) {
        return null;
    }

    /**
     * Adds a query parameter to the request URL.
     *
     * @param name  Parameter name.
     * @param value Parameter value.
     * @since v1.18
     */
    @Override
    public RequestOptions setQueryParam(String name, String value) {
        return null;
    }

    /**
     * Adds a query parameter to the request URL.
     *
     * @param name  Parameter name.
     * @param value Parameter value.
     * @since v1.18
     */
    @Override
    public RequestOptions setQueryParam(String name, boolean value) {
        return null;
    }

    /**
     * Adds a query parameter to the request URL.
     *
     * @param name  Parameter name.
     * @param value Parameter value.
     * @since v1.18
     */
    @Override
    public RequestOptions setQueryParam(String name, int value) {
        return null;
    }

    /**
     * Sets request timeout in milliseconds. Defaults to {@code 30000} (30 seconds). Pass {@code 0} to disable timeout.
     *
     * @param timeout Request timeout in milliseconds.
     * @since v1.18
     */
    @Override
    public RequestOptions setTimeout(double timeout) {
        return null;
    }
}
