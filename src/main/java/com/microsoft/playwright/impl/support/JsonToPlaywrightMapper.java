package com.microsoft.playwright.impl.support;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.microsoft.playwright.ConsoleMessage;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.impl.dto.ConsoleMessageImpl;
import com.microsoft.playwright.impl.dto.ResponseImpl;
import com.microsoft.playwright.options.HttpHeader;
import com.microsoft.playwright.options.SecurityDetails;
import com.microsoft.playwright.options.ServerAddr;
import com.microsoft.playwright.JSHandle;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class JsonToPlaywrightMapper {
    private static final Gson gson = new Gson();

    public static ConsoleMessage mapToConsoleMessage(JsonObject json) {
        if (json == null) {
            return null;
        }

        // Prüfe, ob die Daten innerhalb eines "result"-Objekts liegen
        JsonObject result = json.has("result") ? json.getAsJsonObject("result") : json;

        String text = result.has("message") ? result.get("message").getAsString() : "";
        String type = result.has("type") ? result.get("type").getAsString() : "log";
        String location = result.has("location") ? result.get("location").getAsString() : "";

        Type listType = new TypeToken<List<JSHandle>>() {}.getType();
        List<JSHandle> args = result.has("args") ? gson.fromJson(result.get("args"), listType) : Collections.emptyList();

        return new ConsoleMessageImpl(text, type, location, args, null);
    }

    public static Response mapToResponse(JsonObject json) {
        if (json == null) {
            throw new IllegalArgumentException("Received null JSON for response mapping");
        }

        // Prüfe, ob es sich um eine "success"-Antwort mit einem "result"-Objekt handelt
        JsonObject result = json.has("result") ? json.getAsJsonObject("result") : json;

        // Fallback für "result" oder Standardwerte für fehlende Felder
        String url = result.has("url") ? result.get("url").getAsString() : "unknown";
        int status = result.has("status") ? result.get("status").getAsInt() : 500;
        String statusText = result.has("statusText") ? result.get("statusText").getAsString() : "Unknown Status";

        Type headersType = new TypeToken<Map<String, String>>() {}.getType();
        Map<String, String> headers = result.has("headers") ? gson.fromJson(result.get("headers"), headersType) : Collections.emptyMap();

        Type headerArrayType = new TypeToken<List<HttpHeader>>() {}.getType();
        List<HttpHeader> headersArray = result.has("headersArray") ? gson.fromJson(result.get("headersArray"), headerArrayType) : Collections.emptyList();

        byte[] body = result.has("body") ? result.get("body").getAsString().getBytes() : new byte[0];

        SecurityDetails securityDetails = result.has("securityDetails") ? gson.fromJson(result.get("securityDetails"), SecurityDetails.class) : null;
        ServerAddr serverAddr = result.has("serverAddr") ? gson.fromJson(result.get("serverAddr"), ServerAddr.class) : null;

        boolean fromServiceWorker = result.has("fromServiceWorker") && result.get("fromServiceWorker").getAsBoolean();
        boolean ok = result.has("ok") && result.get("ok").getAsBoolean();

        return new ResponseImpl(url, status, statusText, headers, body, null, null, securityDetails, serverAddr, fromServiceWorker, ok, headersArray);
    }

}
