package de.bund.zrb.event;

import com.microsoft.playwright.Request;
import com.microsoft.playwright.Response;
import de.bund.zrb.type.browsingContext.WDBrowsingContext;
import de.bund.zrb.type.browsingContext.WDNavigation;
import de.bund.zrb.type.network.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Request/Response correlation to ensure:
 * 1. The same Request object is used across onRequest, onRequestFinished, and onRequestFailed events
 * 2. Response.request() returns the correct Request object
 */
class RequestResponseCorrelationTest {

    private WDNetworkEvent.BeforeRequestSent createBeforeRequestSent(String requestId) {
        // Create minimal mock data for testing
        WDRequest request = new WDRequest(requestId);
        List<WDHeader> headers = new ArrayList<>();
        headers.add(new WDHeader("Content-Type", new WDBytesValue.StringValueWD("text/html")));
        
        WDRequestData requestData = new WDRequestData(
            request,
            "https://example.com",
            "GET",
            headers,
            Collections.emptyList(),
            0L,
            0L,
            "document",
            null,
            null
        );
        
        WDBrowsingContext context = new WDBrowsingContext("test-context-1");
        WDNavigation navigation = new WDNavigation("test-nav-1");
        
        WDNetworkEvent.BeforeRequestSent.BeforeRequestSentParametersWD params =
            new WDNetworkEvent.BeforeRequestSent.BeforeRequestSentParametersWD(
                context,
                false,
                navigation,
                0L,
                requestData,
                System.currentTimeMillis(),
                Collections.emptyList(),
                null
            );
        
        // Create event via JSON to properly initialize
        com.google.gson.JsonObject json = new com.google.gson.JsonObject();
        json.addProperty("method", "network.beforeRequestSent");
        
        return new WDNetworkEvent.BeforeRequestSent(json) {
            @Override
            public WDNetworkEvent.BeforeRequestSent.BeforeRequestSentParametersWD getParams() {
                return params;
            }
        };
    }

    private WDNetworkEvent.ResponseCompleted createResponseCompleted(String requestId) {
        WDRequest request = new WDRequest(requestId);
        List<WDHeader> headers = new ArrayList<>();
        headers.add(new WDHeader("Content-Type", new WDBytesValue.StringValueWD("text/html")));
        
        WDRequestData requestData = new WDRequestData(
            request,
            "https://example.com",
            "GET",
            headers,
            Collections.emptyList(),
            0L,
            0L,
            "document",
            null,
            null
        );
        
        WDResponseData responseData = new WDResponseData(
            "https://example.com",
            "http/1.1",
            200,
            "OK",
            false,
            headers,
            "text/html",
            0L,
            0L,
            0L,
            null
        );
        
        WDBrowsingContext context = new WDBrowsingContext("test-context-1");
        WDNavigation navigation = new WDNavigation("test-nav-1");
        
        WDNetworkEvent.ResponseCompleted.ResponseCompletedParametersWD params =
            new WDNetworkEvent.ResponseCompleted.ResponseCompletedParametersWD(
                context,
                false,
                navigation,
                0L,
                requestData,
                System.currentTimeMillis(),
                Collections.emptyList(),
                responseData
            );
        
        com.google.gson.JsonObject json = new com.google.gson.JsonObject();
        json.addProperty("method", "network.responseCompleted");
        
        return new WDNetworkEvent.ResponseCompleted(json) {
            @Override
            public WDNetworkEvent.ResponseCompleted.ResponseCompletedParametersWD getParams() {
                return params;
            }
        };
    }

    private WDNetworkEvent.FetchError createFetchError(String requestId) {
        WDRequest request = new WDRequest(requestId);
        List<WDHeader> headers = new ArrayList<>();
        
        WDRequestData requestData = new WDRequestData(
            request,
            "https://example.com/fail",
            "GET",
            headers,
            Collections.emptyList(),
            0L,
            0L,
            "document",
            null,
            null
        );
        
        WDBrowsingContext context = new WDBrowsingContext("test-context-1");
        WDNavigation navigation = new WDNavigation("test-nav-1");
        
        WDNetworkEvent.FetchError.FetchErrorParametersWD params =
            new WDNetworkEvent.FetchError.FetchErrorParametersWD(
                context,
                false,
                navigation,
                0L,
                requestData,
                System.currentTimeMillis(),
                Collections.emptyList(),
                "Network error"
            );
        
        com.google.gson.JsonObject json = new com.google.gson.JsonObject();
        json.addProperty("method", "network.fetchError");
        
        return new WDNetworkEvent.FetchError(json) {
            @Override
            public WDNetworkEvent.FetchError.FetchErrorParametersWD getParams() {
                return params;
            }
        };
    }

    private WDNetworkEvent.ResponseStarted createResponseStarted(String requestId) {
        WDRequest request = new WDRequest(requestId);
        List<WDHeader> headers = new ArrayList<>();
        headers.add(new WDHeader("Content-Type", new WDBytesValue.StringValueWD("text/html")));
        
        WDRequestData requestData = new WDRequestData(
            request,
            "https://example.com",
            "GET",
            headers,
            Collections.emptyList(),
            0L,
            0L,
            "document",
            null,
            null
        );
        
        WDResponseData responseData = new WDResponseData(
            "https://example.com",
            "http/1.1",
            200,
            "OK",
            false,
            headers,
            "text/html",
            0L,
            0L,
            0L,
            null
        );
        
        WDBrowsingContext context = new WDBrowsingContext("test-context-1");
        WDNavigation navigation = new WDNavigation("test-nav-1");
        
        WDNetworkEvent.ResponseStarted.ResponseStartedParametersWD params =
            new WDNetworkEvent.ResponseStarted.ResponseStartedParametersWD(
                context,
                false,
                navigation,
                0L,
                requestData,
                System.currentTimeMillis(),
                Collections.emptyList(),
                responseData
            );
        
        com.google.gson.JsonObject json = new com.google.gson.JsonObject();
        json.addProperty("method", "network.responseStarted");
        
        return new WDNetworkEvent.ResponseStarted(json) {
            @Override
            public WDNetworkEvent.ResponseStarted.ResponseStartedParametersWD getParams() {
                return params;
            }
        };
    }

    @Test
    void testRequestImplEnrichmentWithResponse() {
        // Create a Request from BeforeRequestSent
        String requestId = "test-request-1";
        WDNetworkEvent.BeforeRequestSent beforeEvent = createBeforeRequestSent(requestId);
        RequestImpl req = new RequestImpl(beforeEvent);
        
        // Initially, response should be null and failure should be empty
        assertNull(req.response());
        assertEquals("", req.failure());
        
        // Enrich with ResponseCompleted
        WDNetworkEvent.ResponseCompleted completedEvent = createResponseCompleted(requestId);
        req.enrichWithResponse(completedEvent);
        
        // Now response should be present
        assertNotNull(req.response());
        assertEquals(200, req.response().status());
        assertEquals("", req.failure()); // failure should still be empty
    }

    @Test
    void testRequestImplEnrichmentWithError() {
        // Create a Request from BeforeRequestSent
        String requestId = "test-request-2";
        WDNetworkEvent.BeforeRequestSent beforeEvent = createBeforeRequestSent(requestId);
        RequestImpl req = new RequestImpl(beforeEvent);
        
        // Initially, failure should be empty
        assertEquals("", req.failure());
        
        // Enrich with FetchError
        WDNetworkEvent.FetchError errorEvent = createFetchError(requestId);
        req.enrichWithError(errorEvent);
        
        // Now failure should contain the error text
        assertEquals("Network error", req.failure());
        assertNull(req.response()); // response should still be null
    }

    @Test
    void testResponseImplRequestReference() {
        // Create a Request
        String requestId = "test-request-3";
        WDNetworkEvent.BeforeRequestSent beforeEvent = createBeforeRequestSent(requestId);
        RequestImpl req = new RequestImpl(beforeEvent);
        
        // Create a Response with the Request reference
        WDNetworkEvent.ResponseStarted responseEvent = createResponseStarted(requestId);
        ResponseImpl resp = new ResponseImpl(responseEvent, null, req);
        
        // Response should reference the Request
        assertNotNull(resp.request());
        assertSame(req, resp.request());
    }

    @Test
    void testResponseImplWithNullRequest() {
        // Edge case: ResponseImpl with null request (should not crash)
        String requestId = "test-request-4";
        WDNetworkEvent.ResponseStarted responseEvent = createResponseStarted(requestId);
        ResponseImpl resp = new ResponseImpl(responseEvent, null, null);
        
        // Response.request() should return null
        assertNull(resp.request());
    }

    @Test
    void testRequestImplCreatedFromBeforeRequestSent() {
        // Verify basic RequestImpl construction
        String requestId = "test-request-5";
        WDNetworkEvent.BeforeRequestSent beforeEvent = createBeforeRequestSent(requestId);
        RequestImpl req = new RequestImpl(beforeEvent);
        
        assertEquals("https://example.com", req.url());
        assertEquals("GET", req.method());
        assertFalse(req.isNavigationRequest());
        assertEquals("", req.failure());
        assertNull(req.response());
    }
}

