package wd4j.impl.playwright.testing;

import com.microsoft.APIResponse;
import com.microsoft.options.HttpHeader;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * NOT IMPLEMENTED YET
 */
public class APIResponseImpl implements APIResponse {
    /**
     * Returns the buffer with response body.
     *
     * @since v1.16
     */
    @Override
    public byte[] body() {
        return new byte[0];
    }

    /**
     * Disposes the body of this response. If not called then the body will stay in memory until the context closes.
     *
     * @since v1.16
     */
    @Override
    public void dispose() {

    }

    /**
     * An object with all the response HTTP headers associated with this response.
     *
     * @since v1.16
     */
    @Override
    public Map<String, String> headers() {
        return Collections.emptyMap();
    }

    /**
     * An array with all the response HTTP headers associated with this response. Header names are not lower-cased. Headers
     * with multiple entries, such as {@code Set-Cookie}, appear in the array multiple times.
     *
     * @since v1.16
     */
    @Override
    public List<HttpHeader> headersArray() {
        return Collections.emptyList();
    }

    /**
     * Contains a boolean stating whether the response was successful (status in the range 200-299) or not.
     *
     * @since v1.16
     */
    @Override
    public boolean ok() {
        return false;
    }

    /**
     * Contains the status code of the response (e.g., 200 for a success).
     *
     * @since v1.16
     */
    @Override
    public int status() {
        return 0;
    }

    /**
     * Contains the status text of the response (e.g. usually an "OK" for a success).
     *
     * @since v1.16
     */
    @Override
    public String statusText() {
        return "";
    }

    /**
     * Returns the text representation of response body.
     *
     * @since v1.16
     */
    @Override
    public String text() {
        return "";
    }

    /**
     * Contains the URL of the response.
     *
     * @since v1.16
     */
    @Override
    public String url() {
        return "";
    }
}
