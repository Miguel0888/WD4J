# Request/Response Correlation in Playwright Adapter

## Overview

The Playwright Adapter now properly implements Request/Response correlation as specified by the Playwright API. This ensures that all network events for a single browser request operate on the same `Request` object, and that `Response.request()` returns the correct `Request` instance.

## Problem Statement

Previously, the adapter had the following issues:

1. **New `RequestImpl` instances for each event**: Each network event (`onRequest`, `onRequestFinished`, `onRequestFailed`) created a new `RequestImpl` object, breaking object identity.

2. **`ResponseImpl.request()` always returned `null`**: There was no connection between Response and Request objects.

3. **No requestId tracking**: There was no internal mapping from BiDi `requestId` to `RequestImpl`.

This violated Playwright's API contract and made it impossible to:
- Track in-flight network activity reliably
- Implement `waitForNetworkIdle`-like behavior
- Correlate requests and responses using object identity
- Use `response.request()` to get the originating request

## Solution

### Architecture

The solution implements requestId-based tracking in `PageImpl`:

```
BeforeRequestSent (onRequest)
    ↓
Create RequestImpl → Store in Map<String, RequestImpl> by requestId
    ↓
ResponseStarted (onResponse)
    ↓
Lookup RequestImpl by requestId → Pass to ResponseImpl
    ↓
ResponseCompleted (onRequestFinished) OR FetchError (onRequestFailed)
    ↓
Lookup RequestImpl by requestId → Enrich with data → Remove from map
```

### Key Components

#### 1. PageImpl Changes

**New field:**
```java
private final Map<String, RequestImpl> requestsById = new ConcurrentHashMap<>();
```

**New helper method:**
```java
private String extractRequestId(WDBaseParameters params)
```
Extracts the BiDi requestId from network event parameters.

**Modified event handlers:**

- **`onRequest`**: Creates `RequestImpl` once and stores it in `requestsById`
- **`onRequestFinished`**: Retrieves cached `RequestImpl`, enriches with response data, then removes from map
- **`onRequestFailed`**: Retrieves cached `RequestImpl`, enriches with error data, then removes from map  
- **`onResponse`**: Retrieves cached `RequestImpl` and passes it to `ResponseImpl`

#### 2. RequestImpl Changes

**Made mutable for enrichment:**
```java
private String failure;          // was: private final String failure;
private Response response;       // was: private final Response response;
```

**New enrichment methods:**
```java
public void enrichWithResponse(WDNetworkEvent.ResponseCompleted completed)
public void enrichWithError(WDNetworkEvent.FetchError fetchError)
```

These methods allow a single `RequestImpl` to be updated when the request completes or fails, without creating a new object.

#### 3. ResponseImpl Changes

**Modified constructor:**
```java
public ResponseImpl(WDNetworkEvent.ResponseStarted event, byte[] responseBody, Request request)
```

Now accepts the `Request` reference and stores it, so `response.request()` returns the correct object.

## Usage Example

```java
Page page = browser.newPage();

// Track the same Request object across events
final Request[] seenRequest = new Request[1];

page.onRequest(request -> {
    seenRequest[0] = request;
    System.out.println("Request started: " + request.url());
});

page.onRequestFinished(request -> {
    // Same object as in onRequest
    assert seenRequest[0] == request;
    System.out.println("Request finished: " + request.url());
});

page.onResponse(response -> {
    // Response references the same Request
    assert seenRequest[0] == response.request();
    System.out.println("Response received: " + response.status());
});

page.navigate("https://example.com");
```

## Testing

Comprehensive unit tests are provided in `RequestResponseCorrelationTest.java`:

- **testRequestImplEnrichmentWithResponse**: Verifies Request can be enriched with response data
- **testRequestImplEnrichmentWithError**: Verifies Request can be enriched with error data
- **testResponseImplRequestReference**: Verifies Response.request() returns the correct Request
- **testResponseImplWithNullRequest**: Edge case handling
- **testRequestImplCreatedFromBeforeRequestSent**: Basic construction verification

All tests pass successfully.

## Architecture Compliance

✅ All changes are in the `playwright-adapter` module  
✅ No Playwright-specific code in the `wd4j` module  
✅ Uses existing WebDriver BiDi types and events from `wd4j`  
✅ Maintains clean separation of concerns

## Performance Considerations

- **Memory**: The `requestsById` map is bounded by the number of in-flight requests. Completed requests are immediately removed from the map.
- **Thread-safety**: Uses `ConcurrentHashMap` for thread-safe access.
- **Cleanup**: Requests are removed from the map in both success (`onRequestFinished`) and failure (`onRequestFailed`) paths.

## Edge Cases Handled

1. **Missing requestId**: Falls back to creating a new `RequestImpl` without caching
2. **Response before Request**: `ResponseImpl` accepts null Request reference
3. **Request without completion**: Map cleanup happens on both success and failure paths
4. **Null parameters**: All methods check for null before accessing fields

## Future Enhancements

Potential improvements for consideration:

1. **Timeout-based cleanup**: Add a background task to clean up stale entries in `requestsById` if events are lost
2. **Metrics**: Track map size and request lifecycle duration for debugging
3. **Request chaining**: Support for redirect chains where multiple requests share a relationship

## References

- [Playwright Request API](https://playwright.dev/java/docs/api/class-request)
- [Playwright Response API](https://playwright.dev/java/docs/api/class-response)
- [WebDriver BiDi Network Events](https://w3c.github.io/webdriver-bidi/#module-network)
