package testing;

import com.microsoft.APIRequestContext;
import com.microsoft.APIResponse;
import com.microsoft.Request;
import com.microsoft.options.RequestOptions;

/**
 * NOT IMPLEMENTED YET
 */
public class APIRequestContextImpl implements APIRequestContext {
    /**
     * Sends HTTP(S) <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Methods/DELETE">DELETE</a> request and returns
     * its response. The method will populate request cookies from the context and update context cookies from the response.
     * The method will automatically follow redirects.
     *
     * @param url    Target URL.
     * @param params Optional request parameters.
     * @since v1.16
     */
    @Override
    public APIResponse delete(String url, RequestOptions params) {
        return null;
    }

    /**
     * All responses returned by {@link APIRequestContext#get APIRequestContext.get()} and similar
     * methods are stored in the memory, so that you can later call {@link APIResponse#body
     * APIResponse.body()}.This method discards all its resources, calling any method on disposed {@code APIRequestContext}
     * will throw an exception.
     *
     * @param options
     * @since v1.16
     */
    @Override
    public void dispose(DisposeOptions options) {

    }

    /**
     * Sends HTTP(S) request and returns its response. The method will populate request cookies from the context and update
     * context cookies from the response. The method will automatically follow redirects.
     *
     * <p> <strong>Usage</strong>
     *
     * <p> JSON objects can be passed directly to the request:
     * <pre>{@code
     * Map<String, Object> data = new HashMap();
     * data.put("title", "Book Title");
     * data.put("body", "John Doe");
     * request.fetch("https://example.com/api/createBook", RequestOptions.create().setMethod("post").setData(data));
     * }</pre>
     *
     * <p> The common way to send file(s) in the body of a request is to upload them as form fields with {@code
     * multipart/form-data} encoding, by specifiying the {@code multipart} parameter:
     * <pre>{@code
     * // Pass file path to the form data constructor:
     * Path file = Paths.get("team.csv");
     * APIResponse response = request.fetch("https://example.com/api/uploadTeamList",
     *   RequestOptions.create().setMethod("post").setMultipart(
     *     FormData.create().set("fileField", file)));
     *
     * // Or you can pass the file content directly as FilePayload object:
     * FilePayload filePayload = new FilePayload("f.js", "text/javascript",
     *       "console.log(2022);".getBytes(StandardCharsets.UTF_8));
     * APIResponse response = request.fetch("https://example.com/api/uploadScript",
     *   RequestOptions.create().setMethod("post").setMultipart(
     *     FormData.create().set("fileField", filePayload)));
     * }</pre>
     *
     * @param urlOrRequest Target URL or Request to get all parameters from.
     * @param params       Optional request parameters.
     * @since v1.16
     */
    @Override
    public APIResponse fetch(String urlOrRequest, RequestOptions params) {
        return null;
    }

    /**
     * Sends HTTP(S) request and returns its response. The method will populate request cookies from the context and update
     * context cookies from the response. The method will automatically follow redirects.
     *
     * <p> <strong>Usage</strong>
     *
     * <p> JSON objects can be passed directly to the request:
     * <pre>{@code
     * Map<String, Object> data = new HashMap();
     * data.put("title", "Book Title");
     * data.put("body", "John Doe");
     * request.fetch("https://example.com/api/createBook", RequestOptions.create().setMethod("post").setData(data));
     * }</pre>
     *
     * <p> The common way to send file(s) in the body of a request is to upload them as form fields with {@code
     * multipart/form-data} encoding, by specifiying the {@code multipart} parameter:
     * <pre>{@code
     * // Pass file path to the form data constructor:
     * Path file = Paths.get("team.csv");
     * APIResponse response = request.fetch("https://example.com/api/uploadTeamList",
     *   RequestOptions.create().setMethod("post").setMultipart(
     *     FormData.create().set("fileField", file)));
     *
     * // Or you can pass the file content directly as FilePayload object:
     * FilePayload filePayload = new FilePayload("f.js", "text/javascript",
     *       "console.log(2022);".getBytes(StandardCharsets.UTF_8));
     * APIResponse response = request.fetch("https://example.com/api/uploadScript",
     *   RequestOptions.create().setMethod("post").setMultipart(
     *     FormData.create().set("fileField", filePayload)));
     * }</pre>
     *
     * @param urlOrRequest Target URL or Request to get all parameters from.
     * @param params       Optional request parameters.
     * @since v1.16
     */
    @Override
    public APIResponse fetch(Request urlOrRequest, RequestOptions params) {
        return null;
    }

    /**
     * Sends HTTP(S) <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Methods/GET">GET</a> request and returns its
     * response. The method will populate request cookies from the context and update context cookies from the response. The
     * method will automatically follow redirects.
     *
     * <p> <strong>Usage</strong>
     *
     * <p> Request parameters can be configured with {@code params} option, they will be serialized into the URL search parameters:
     * <pre>{@code
     * request.get("https://example.com/api/getText", RequestOptions.create()
     *   .setQueryParam("isbn", "1234")
     *   .setQueryParam("page", 23));
     * }</pre>
     *
     * @param url    Target URL.
     * @param params Optional request parameters.
     * @since v1.16
     */
    @Override
    public APIResponse get(String url, RequestOptions params) {
        return null;
    }

    /**
     * Sends HTTP(S) <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Methods/HEAD">HEAD</a> request and returns its
     * response. The method will populate request cookies from the context and update context cookies from the response. The
     * method will automatically follow redirects.
     *
     * @param url    Target URL.
     * @param params Optional request parameters.
     * @since v1.16
     */
    @Override
    public APIResponse head(String url, RequestOptions params) {
        return null;
    }

    /**
     * Sends HTTP(S) <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Methods/PATCH">PATCH</a> request and returns
     * its response. The method will populate request cookies from the context and update context cookies from the response.
     * The method will automatically follow redirects.
     *
     * @param url    Target URL.
     * @param params Optional request parameters.
     * @since v1.16
     */
    @Override
    public APIResponse patch(String url, RequestOptions params) {
        return null;
    }

    /**
     * Sends HTTP(S) <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Methods/POST">POST</a> request and returns its
     * response. The method will populate request cookies from the context and update context cookies from the response. The
     * method will automatically follow redirects.
     *
     * <p> <strong>Usage</strong>
     *
     * <p> JSON objects can be passed directly to the request:
     * <pre>{@code
     * Map<String, Object> data = new HashMap();
     * data.put("title", "Book Title");
     * data.put("body", "John Doe");
     * request.post("https://example.com/api/createBook", RequestOptions.create().setData(data));
     * }</pre>
     *
     * <p> To send form data to the server use {@code form} option. Its value will be encoded into the request body with {@code
     * application/x-www-form-urlencoded} encoding (see below how to use {@code multipart/form-data} form encoding to send
     * files):
     * <pre>{@code
     * request.post("https://example.com/api/findBook", RequestOptions.create().setForm(
     *     FormData.create().set("title", "Book Title").set("body", "John Doe")
     * ));
     * }</pre>
     *
     * <p> The common way to send file(s) in the body of a request is to upload them as form fields with {@code
     * multipart/form-data} encoding. Use {@code FormData} to construct request body and pass it to the request as {@code
     * multipart} parameter:
     * <pre>{@code
     * // Pass file path to the form data constructor:
     * Path file = Paths.get("team.csv");
     * APIResponse response = request.post("https://example.com/api/uploadTeamList",
     *   RequestOptions.create().setMultipart(
     *     FormData.create().set("fileField", file)));
     *
     * // Or you can pass the file content directly as FilePayload object:
     * FilePayload filePayload1 = new FilePayload("f1.js", "text/javascript",
     *       "console.log(2022);".getBytes(StandardCharsets.UTF_8));
     * APIResponse response = request.post("https://example.com/api/uploadScript",
     *   RequestOptions.create().setMultipart(
     *     FormData.create().set("fileField", filePayload)));
     * }</pre>
     *
     * @param url    Target URL.
     * @param params Optional request parameters.
     * @since v1.16
     */
    @Override
    public APIResponse post(String url, RequestOptions params) {
        return null;
    }

    /**
     * Sends HTTP(S) <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Methods/PUT">PUT</a> request and returns its
     * response. The method will populate request cookies from the context and update context cookies from the response. The
     * method will automatically follow redirects.
     *
     * @param url    Target URL.
     * @param params Optional request parameters.
     * @since v1.16
     */
    @Override
    public APIResponse put(String url, RequestOptions params) {
        return null;
    }

    /**
     * Returns storage state for this request context, contains current cookies and local storage snapshot if it was passed to
     * the constructor.
     *
     * @param options
     * @since v1.16
     */
    @Override
    public String storageState(StorageStateOptions options) {
        return "";
    }
}
