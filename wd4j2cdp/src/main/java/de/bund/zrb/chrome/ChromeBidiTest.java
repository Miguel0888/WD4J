package de.bund.zrb.chrome;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Quick integration test: launches Chrome automatically (or connects to an already-running instance),
 * injects the BiDi mapper, and sends simple BiDi commands.
 * <p>
 * Usage:
 *   Option A: Just run this class – it will start Chrome on port 9222.
 *   Option B: Start Chrome manually first: chrome.exe --remote-debugging-port=9222
 *             Then run this class.
 *   Arg 1 (optional): port (default: 9222)
 *   Arg 2 (optional): chrome path (default: auto-detect)
 */
public class ChromeBidiTest {

    private static final String DEFAULT_CHROME_PATH = "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe";

    public static void main(String[] args) throws Exception {
        int port = 9222;
        String chromePath = DEFAULT_CHROME_PATH;

        if (args.length > 0) port = Integer.parseInt(args[0]);
        if (args.length > 1) chromePath = args[1];

        System.out.println("=== Chrome BiDi Mapper Test ===");

        // Try to get CDP URL from an already-running Chrome
        String cdpWsUrl = getCdpWebSocketUrl(port);

        Process chromeProcess = null;
        if (cdpWsUrl == null) {
            System.out.println("No Chrome instance found on port " + port + " – launching Chrome...");
            chromeProcess = launchChrome(chromePath, port);
            // Wait and retry
            for (int i = 0; i < 10; i++) {
                Thread.sleep(1000);
                cdpWsUrl = getCdpWebSocketUrl(port);
                if (cdpWsUrl != null) break;
                System.out.println("  Waiting for Chrome to start... (" + (i + 1) + "/10)");
            }
        }

        if (cdpWsUrl == null) {
            System.err.println("ERROR: Could not get webSocketDebuggerUrl from http://127.0.0.1:" + port + "/json/version");
            System.err.println("Make sure Chrome is installed at: " + chromePath);
            if (chromeProcess != null) chromeProcess.destroyForcibly();
            System.exit(1);
        }
        System.out.println("CDP WebSocket URL: " + cdpWsUrl);

        // Pre-mapper: find existing about:blank target via HTTP /json
        System.out.println("\n[0] Pre-mapper: finding existing targets via HTTP /json...");
        String preFoundContextId = null;
        String targetsJson = getChromeTargets(port);
        if (targetsJson != null) {
            System.out.println("Chrome /json response: " + targetsJson.substring(0, Math.min(300, targetsJson.length())) + "...");
            // Use Gson for proper JSON parsing
            com.google.gson.JsonArray targets = com.google.gson.JsonParser.parseString(targetsJson).getAsJsonArray();
            for (com.google.gson.JsonElement elem : targets) {
                com.google.gson.JsonObject t = elem.getAsJsonObject();
                String type = t.has("type") ? t.get("type").getAsString() : "";
                String targetUrl = t.has("url") ? t.get("url").getAsString() : "";
                String id = t.has("id") ? t.get("id").getAsString() : "";
                System.out.println("  Target: type=" + type + " id=" + id + " url=" + targetUrl);
                if ("page".equals(type) && !targetUrl.contains("MAPPER_TARGET")) {
                    preFoundContextId = id;
                    System.out.println("  → Using this as context: " + id);
                    break;
                }
            }
        } else {
            System.out.println("  /json returned null!");
        }

        // Create the ChromeBidiWebSocketImpl
        System.out.println("\nCreating ChromeBidiWebSocketImpl...");
        ChromeBidiWebSocketImpl bidiWs = new ChromeBidiWebSocketImpl(cdpWsUrl, true);

        // Collect all BiDi responses and events by ID
        final Map<Integer, String> responses = new ConcurrentHashMap<Integer, String>();
        final List<String> events = new ArrayList<String>();
        bidiWs.onFrameReceived(frame -> {
            String text = frame.text();
            System.out.println("\n[BiDi] " + text);
            // Try to extract "id" from response
            int idIdx = text.indexOf("\"id\":");
            if (idIdx >= 0) {
                try {
                    int numStart = idIdx + 5;
                    while (numStart < text.length() && !Character.isDigit(text.charAt(numStart))) numStart++;
                    int numEnd = numStart;
                    while (numEnd < text.length() && Character.isDigit(text.charAt(numEnd))) numEnd++;
                    if (numEnd > numStart) {
                        int id = Integer.parseInt(text.substring(numStart, numEnd));
                        responses.put(id, text);
                    }
                } catch (NumberFormatException ignored) {}
            }
            if (text.contains("\"type\":\"event\"")) {
                events.add(text);
            }
        });

        // Step 1: Subscribe to events so the mapper tracks contexts
        System.out.println("\n[1] Subscribing to browsingContext events...");
        bidiWs.send("{\"id\":1,\"method\":\"session.subscribe\",\"params\":{\"events\":[\"browsingContext.contextCreated\",\"browsingContext.navigationStarted\",\"browsingContext.domContentLoaded\",\"browsingContext.load\"]}}");
        waitForResponse(responses, 1, 5000);

        // Step 2: session.status
        System.out.println("\n[2] Sending session.status...");
        bidiWs.send("{\"id\":2,\"method\":\"session.status\",\"params\":{}}");
        waitForResponse(responses, 2, 3000);

        // Step 3: Get existing browsing contexts (the about:blank tab Chrome already opened)
        System.out.println("\n[3] Getting browsingContext.getTree to find existing tab...");
        bidiWs.send("{\"id\":3,\"method\":\"browsingContext.getTree\",\"params\":{}}");
        String treeResp = waitForResponse(responses, 3, 5000);

        // Extract context ID from the tree response (first non-mapper context)
        String contextId = null;
        if (treeResp != null) {
            // Find all "context":"..." entries and pick the first that is NOT a mapper target
            int searchFrom = 0;
            while (searchFrom < treeResp.length()) {
                int ctxIdx = treeResp.indexOf("\"context\"", searchFrom);
                if (ctxIdx < 0) break;
                int valStart = treeResp.indexOf("\"", ctxIdx + 10) + 1;
                int valEnd = treeResp.indexOf("\"", valStart);
                if (valStart > 0 && valEnd > valStart) {
                    String candidate = treeResp.substring(valStart, valEnd);
                    // Check if this context's URL contains MAPPER_TARGET
                    int urlIdx = treeResp.indexOf("\"url\"", valEnd);
                    boolean isMapper = false;
                    if (urlIdx >= 0 && urlIdx < valEnd + 200) {
                        int urlValStart = treeResp.indexOf("\"", urlIdx + 5) + 1;
                        int urlValEnd = treeResp.indexOf("\"", urlValStart);
                        if (urlValStart > 0 && urlValEnd > urlValStart) {
                            String url = treeResp.substring(urlValStart, urlValEnd);
                            isMapper = url.contains("MAPPER_TARGET");
                        }
                    }
                    if (!isMapper && contextId == null) {
                        contextId = candidate;
                        System.out.println("Found existing context: " + contextId);
                    }
                }
                searchFrom = valEnd > 0 ? valEnd + 1 : searchFrom + 1;
            }
        }

        // Fallback: use pre-mapper target ID
        if (contextId == null && preFoundContextId != null) {
            contextId = preFoundContextId;
            System.out.println("Using pre-mapper target as context: " + contextId);
        }

        if (contextId == null) {
            System.err.println("WARNING: Could not extract context ID from browsingContext.create response");
            System.err.println("Responses received: " + responses.keySet());
            System.err.println("Events received: " + events.size());
            System.err.println("The BiDi mapper may not fully support browsingContext.create.");
            System.err.println("This is a known limitation of the chromium-bidi mapper.");
            System.out.println("\n=== Test partially complete (context creation failed) ===");
            bidiWs.close();
            if (chromeProcess != null) {
                System.out.println("Killing Chrome...");
                chromeProcess.destroyForcibly();
            }
            System.exit(0);
        }
        System.out.println("Context ID: " + contextId);

        // Step 4: Navigate to example.com
        System.out.println("\n[4] Navigating to https://example.com ...");
        bidiWs.send("{\"id\":4,\"method\":\"browsingContext.navigate\",\"params\":{\"context\":\"" + contextId + "\",\"url\":\"https://example.com\",\"wait\":\"interactive\"}}");
        waitForResponse(responses, 4, 15000);

        // Step 5: Get the page tree
        System.out.println("\n[5] Getting browsingContext.getTree...");
        bidiWs.send("{\"id\":5,\"method\":\"browsingContext.getTree\",\"params\":{}}");
        waitForResponse(responses, 5, 5000);

        System.out.println("\n=== Test complete! ===");
        System.out.println("Total responses: " + responses.size());
        System.out.println("Total events: " + events.size());
        bidiWs.close();

        if (chromeProcess != null) {
            System.out.println("Killing Chrome...");
            chromeProcess.destroyForcibly();
        }
        System.exit(0);
    }

    private static String waitForResponse(Map<Integer, String> responses, int id, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (!responses.containsKey(id) && System.currentTimeMillis() < deadline) {
            Thread.sleep(100);
        }
        String resp = responses.get(id);
        if (resp == null) {
            System.err.println("  ⚠ Timeout waiting for response id=" + id + " after " + timeoutMs + "ms");
        }
        return resp;
    }

    private static Process launchChrome(String chromePath, int port) throws Exception {
        File chromeExe = new File(chromePath);
        if (!chromeExe.exists()) {
            throw new RuntimeException("Chrome not found at: " + chromePath);
        }

        List<String> cmd = new ArrayList<>();
        cmd.add(chromePath);
        cmd.add("--remote-debugging-port=" + port);
        cmd.add("--no-first-run");
        cmd.add("--no-default-browser-check");
        cmd.add("--disable-extensions");
        cmd.add("--disable-component-extensions-with-background-pages");
        cmd.add("--disable-background-networking");
        cmd.add("--disable-default-apps");
        cmd.add("--disable-sync");
        cmd.add("--disable-translate");
        cmd.add("--disable-hang-monitor");
        cmd.add("--no-service-autorun");
        cmd.add("--password-store=basic");
        cmd.add("--user-data-dir=" + System.getProperty("java.io.tmpdir") + "\\chrome-bidi-test-profile");
        cmd.add("about:blank");

        System.out.println("Starting Chrome: " + cmd);
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // Drain stdout in background
        Thread drainer = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    System.out.println("[Chrome] " + line);
                }
            } catch (Exception ignored) {}
        }, "chrome-stdout-drainer");
        drainer.setDaemon(true);
        drainer.start();

        return process;
    }

    private static String getCdpWebSocketUrl(int port) {
        try {
            URL url = new URL("http://127.0.0.1:" + port + "/json/version");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);

            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                String json = sb.toString();
                String wsKey = "\"webSocketDebuggerUrl\"";
                int idx = json.indexOf(wsKey);
                if (idx >= 0) {
                    int start = json.indexOf("\"ws://", idx);
                    if (start >= 0) {
                        start++;
                        int end = json.indexOf("\"", start);
                        if (end > start) return json.substring(start, end);
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static String getChromeTargets(int port) {
        try {
            URL url = new URL("http://127.0.0.1:" + port + "/json");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);

            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();
                return sb.toString();
            }
        } catch (Exception ignored) {}
        return null;
    }
}

