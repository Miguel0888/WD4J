package de.bund.zrb.chrome.cdp;

import com.google.gson.JsonObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Sets up the chromium-bidi mapper inside a hidden Chrome tab via CDP.
 * <p>
 * This follows the protocol used by GoogleChromeLabs/chromium-bidi:
 * <ol>
 *   <li>Create a hidden mapper tab</li>
 *   <li>Attach to it (flatten: true)</li>
 *   <li>Enable Runtime on the tab</li>
 *   <li>Expose DevTools Protocol to the tab</li>
 *   <li>Add bindings for BiDi responses</li>
 *   <li>Evaluate the bundled mapperTab.js</li>
 *   <li>Run the mapper instance</li>
 * </ol>
 */
public class CdpMapperSetup {

    private static final long COMMAND_TIMEOUT_SECONDS = 30;

    /**
     * Result of the mapper setup: the CDP session ID and target ID of the mapper tab.
     */
    public static class MapperHandle {
        public final String sessionId;
        public final String targetId;

        public MapperHandle(String sessionId, String targetId) {
            this.sessionId = sessionId;
            this.targetId = targetId;
        }
    }

    /**
     * Performs the full mapper setup on an open CDP connection.
     *
     * @param cdp  an open CdpConnection to the Chrome browser
     * @param verbose  if true, also binds "sendDebugMessage" for debug output
     * @return a MapperHandle with sessionId and targetId
     */
    public static MapperHandle setupMapper(CdpConnection cdp, boolean verbose) throws Exception {
        System.out.println("[ChromeBidi] Setting up BiDi mapper...");

        // Step 1: Create hidden mapper tab
        JsonObject createParams = new JsonObject();
        createParams.addProperty("url", "about:blank#MAPPER_TARGET");
        // Chrome-specific: newWindow + background to create a hidden tab
        createParams.addProperty("newWindow", false);
        createParams.addProperty("background", true);

        JsonObject createResult = cdp.sendCommand("Target.createTarget", createParams)
                .get(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        String targetId = createResult.get("targetId").getAsString();
        System.out.println("[ChromeBidi] Mapper target created: " + targetId);

        // Step 2: Attach to target
        JsonObject attachParams = new JsonObject();
        attachParams.addProperty("targetId", targetId);
        attachParams.addProperty("flatten", true);

        JsonObject attachResult = cdp.sendCommand("Target.attachToTarget", attachParams)
                .get(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        String sessionId = attachResult.get("sessionId").getAsString();
        System.out.println("[ChromeBidi] Attached to mapper target, sessionId=" + sessionId);

        // Step 3: Enable Runtime on the mapper tab session
        cdp.sendCommand("Runtime.enable", null, sessionId)
                .get(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        System.out.println("[ChromeBidi] Runtime.enable done");

        // Step 4: Expose DevTools Protocol to the mapper tab
        JsonObject exposeParams = new JsonObject();
        exposeParams.addProperty("bindingName", "cdp");
        exposeParams.addProperty("targetId", targetId);
        // Note: 'inheritPermissions' is a newer Chrome feature, may not be needed in all versions
        cdp.sendCommand("Target.exposeDevToolsProtocol", exposeParams)
                .get(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        System.out.println("[ChromeBidi] Target.exposeDevToolsProtocol done");

        // Step 5: Add binding for BiDi responses
        JsonObject bindingParams = new JsonObject();
        bindingParams.addProperty("name", "sendBidiResponse");
        cdp.sendCommand("Runtime.addBinding", bindingParams, sessionId)
                .get(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        System.out.println("[ChromeBidi] Runtime.addBinding(sendBidiResponse) done");

        // Step 5b: Optional debug binding
        if (verbose) {
            JsonObject debugBindingParams = new JsonObject();
            debugBindingParams.addProperty("name", "sendDebugMessage");
            cdp.sendCommand("Runtime.addBinding", debugBindingParams, sessionId)
                    .get(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            System.out.println("[ChromeBidi] Runtime.addBinding(sendDebugMessage) done");
        }

        // Step 6: Load and inject the mapper source
        String mapperSource = loadMapperSource();
        System.out.println("[ChromeBidi] Mapper source loaded (" + mapperSource.length() + " chars)");

        JsonObject evalParams = new JsonObject();
        evalParams.addProperty("expression", mapperSource);
        cdp.sendCommand("Runtime.evaluate", evalParams, sessionId)
                .get(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        System.out.println("[ChromeBidi] Mapper source injected");

        // Step 7: Run the mapper instance
        JsonObject runParams = new JsonObject();
        runParams.addProperty("expression", "window.runMapperInstance('" + targetId + "')");
        runParams.addProperty("awaitPromise", true);
        cdp.sendCommand("Runtime.evaluate", runParams, sessionId)
                .get(60, TimeUnit.SECONDS); // Mapper init can take a bit
        System.out.println("[ChromeBidi] Mapper instance started!");


        return new MapperHandle(sessionId, targetId);
    }

    /**
     * Loads the bundled mapperTab.js from classpath resources.
     */
    private static String loadMapperSource() {
        try (InputStream is = CdpMapperSetup.class.getClassLoader().getResourceAsStream("chrome-bidi/mapperTab.js")) {
            if (is == null) {
                throw new FileNotFoundException("chrome-bidi/mapperTab.js not found on classpath");
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load mapperTab.js", e);
        }
    }
}

