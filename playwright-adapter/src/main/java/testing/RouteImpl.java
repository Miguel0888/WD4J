package testing;

import com.microsoft.APIResponse;
import com.microsoft.Request;
import com.microsoft.Route;

/**
 * NOT IMPLEMENTED. Route provides a way to intercept network requests and take action based on the request.
 * @link https://w3c.github.io/webdriver-bidi/#command-network-addIntercept
 */
public class RouteImpl implements Route {
    /**
     * Aborts the route's request.
     *
     * @param errorCode Optional error code. Defaults to {@code failed}, could be one of the following:
     *                  <ul>
     *                  <li> {@code "aborted"} - An operation was aborted (due to user action)</li>
     *                  <li> {@code "accessdenied"} - Permission to access a resource, other than the network, was denied</li>
     *                  <li> {@code "addressunreachable"} - The IP address is unreachable. This usually means that there is no route to the specified
     *                  host or network.</li>
     *                  <li> {@code "blockedbyclient"} - The client chose to block the request.</li>
     *                  <li> {@code "blockedbyresponse"} - The request failed because the response was delivered along with requirements which are
     *                  not met ('X-Frame-Options' and 'Content-Security-Policy' ancestor checks, for instance).</li>
     *                  <li> {@code "connectionaborted"} - A connection timed out as a result of not receiving an ACK for data sent.</li>
     *                  <li> {@code "connectionclosed"} - A connection was closed (corresponding to a TCP FIN).</li>
     *                  <li> {@code "connectionfailed"} - A connection attempt failed.</li>
     *                  <li> {@code "connectionrefused"} - A connection attempt was refused.</li>
     *                  <li> {@code "connectionreset"} - A connection was reset (corresponding to a TCP RST).</li>
     *                  <li> {@code "internetdisconnected"} - The Internet connection has been lost.</li>
     *                  <li> {@code "namenotresolved"} - The host name could not be resolved.</li>
     *                  <li> {@code "timedout"} - An operation timed out.</li>
     *                  <li> {@code "failed"} - A generic failure occurred.</li>
     *                  </ul>
     * @since v1.8
     */
    @Override
    public void abort(String errorCode) {

    }

    /**
     * Sends route's request to the network with optional overrides.
     *
     * <p> <strong>Usage</strong>
     * <pre>{@code
     * page.route("**\/*", route -> {
     *   // Override headers
     *   Map<String, String> headers = new HashMap<>(route.request().headers());
     *   headers.put("foo", "foo-value"); // set "foo" header
     *   headers.remove("bar"); // remove "bar" header
     *   route.resume(new Route.ResumeOptions().setHeaders(headers));
     * });
     * }</pre>
     *
     * <p> <strong>Details</strong>
     *
     * <p> The {@code headers} option applies to both the routed request and any redirects it initiates. However, {@code url},
     * {@code method}, and {@code postData} only apply to the original request and are not carried over to redirected requests.
     *
     * <p> {@link Route#resume Route.resume()} will immediately send the request to the network, other
     * matching handlers won't be invoked. Use {@link Route#fallback Route.fallback()} If you want
     * next matching handler in the chain to be invoked.
     *
     * @param options
     * @since v1.8
     */
    @Override
    public void resume(ResumeOptions options) {

    }

    /**
     * Continues route's request with optional overrides. The method is similar to {@link Route#resume
     * Route.resume()} with the difference that other matching handlers will be invoked before sending the request.
     *
     * <p> <strong>Usage</strong>
     *
     * <p> When several routes match the given pattern, they run in the order opposite to their registration. That way the last
     * registered route can always override all the previous ones. In the example below, request will be handled by the
     * bottom-most handler first, then it'll fall back to the previous one and in the end will be aborted by the first
     * registered route.
     * <pre>{@code
     * page.route("**\/*", route -> {
     *   // Runs last.
     *   route.abort();
     * });
     *
     * page.route("**\/*", route -> {
     *   // Runs second.
     *   route.fallback();
     * });
     *
     * page.route("**\/*", route -> {
     *   // Runs first.
     *   route.fallback();
     * });
     * }</pre>
     *
     * <p> Registering multiple routes is useful when you want separate handlers to handle different kinds of requests, for example
     * API calls vs page resources or GET requests vs POST requests as in the example below.
     * <pre>{@code
     * // Handle GET requests.
     * page.route("**\/*", route -> {
     *   if (!route.request().method().equals("GET")) {
     *     route.fallback();
     *     return;
     *   }
     *   // Handling GET only.
     *   // ...
     * });
     *
     * // Handle POST requests.
     * page.route("**\/*", route -> {
     *   if (!route.request().method().equals("POST")) {
     *     route.fallback();
     *     return;
     *   }
     *   // Handling POST only.
     *   // ...
     * });
     * }</pre>
     *
     * <p> One can also modify request while falling back to the subsequent handler, that way intermediate route handler can modify
     * url, method, headers and postData of the request.
     * <pre>{@code
     * page.route("**\/*", route -> {
     *   // Override headers
     *   Map<String, String> headers = new HashMap<>(route.request().headers());
     *   headers.put("foo", "foo-value"); // set "foo" header
     *   headers.remove("bar"); // remove "bar" header
     *   route.fallback(new Route.ResumeOptions().setHeaders(headers));
     * });
     * }</pre>
     *
     * <p> Use {@link Route#resume Route.resume()} to immediately send the request to the network, other
     * matching handlers won't be invoked in that case.
     *
     * @param options
     * @since v1.23
     */
    @Override
    public void fallback(FallbackOptions options) {

    }

    /**
     * Performs the request and fetches result without fulfilling it, so that the response could be modified and then
     * fulfilled.
     *
     * <p> <strong>Usage</strong>
     * <pre>{@code
     * page.route("https://dog.ceo/api/breeds/list/all", route -> {
     *   APIResponse response = route.fetch();
     *   JsonObject json = new Gson().fromJson(response.text(), JsonObject.class);
     *   JsonObject message = itemObj.get("json").getAsJsonObject();
     *   message.set("big_red_dog", new JsonArray());
     *   route.fulfill(new Route.FulfillOptions()
     *     .setResponse(response)
     *     .setBody(json.toString()));
     * });
     * }</pre>
     *
     * <p> <strong>Details</strong>
     *
     * <p> Note that {@code headers} option will apply to the fetched request as well as any redirects initiated by it. If you want
     * to only apply {@code headers} to the original request, but not to redirects, look into {@link
     * Route#resume Route.resume()} instead.
     *
     * @param options
     * @since v1.29
     */
    @Override
    public APIResponse fetch(FetchOptions options) {
        return null;
    }

    /**
     * Fulfills route's request with given response.
     *
     * <p> <strong>Usage</strong>
     *
     * <p> An example of fulfilling all requests with 404 responses:
     * <pre>{@code
     * page.route("**\/*", route -> {
     *   route.fulfill(new Route.FulfillOptions()
     *     .setStatus(404)
     *     .setContentType("text/plain")
     *     .setBody("Not Found!"));
     * });
     * }</pre>
     *
     * <p> An example of serving static file:
     * <pre>{@code
     * page.route("**\/xhr_endpoint", route -> route.fulfill(
     *   new Route.FulfillOptions().setPath(Paths.get("mock_data.json"))));
     * }</pre>
     *
     * @param options
     * @since v1.8
     */
    @Override
    public void fulfill(FulfillOptions options) {

    }

    /**
     * A request to be routed.
     *
     * @since v1.8
     */
    @Override
    public Request request() {
        return null;
    }
}
