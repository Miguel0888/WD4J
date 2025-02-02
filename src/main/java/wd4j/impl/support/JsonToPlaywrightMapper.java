
package wd4j.impl.support;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import wd4j.api.JSHandle;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Utility class that dynamically maps JSON objects to Playwright interfaces.
 * <p>
 * This class eliminates the need for concrete implementations of Playwright interfaces like {@code ConsoleMessage}
 * and {@code Response} by generating anonymous proxy classes at runtime using Java Reflection.
 * </p>
 * <p>
 * The core mechanism is based on {@code Proxy.newProxyInstance()}, which creates a dynamic implementation
 * of the requested interface and maps method calls to JSON fields. This allows Playwright events and responses
 * to be processed without defining explicit implementation classes.
 * </p>
 *
 * <h2>Usage</h2>
 * Example: Mapping a JSON event to a {@code ConsoleMessage} interface:
 * <pre>{@code
 * JsonObject json = new JsonObject();
 * json.addProperty("message", "Console log from page");
 * json.addProperty("type", "log");
 *
 * ConsoleMessage consoleMessage = JsonToPlaywrightMapper.mapToInterface(json, ConsoleMessage.class);
 * System.out.println(consoleMessage.text());  // Output: Console log from page
 * }</pre>
 *
 * <h2>Supported Features</h2>
 * <ul>
 *     <li>Automatic mapping of JSON fields to Playwright interfaces.</li>
 *     <li>Support for nested JSON structures inside "result" objects.</li>
 *     <li>Handling of {@code List<JSHandle>} for Playwright console messages.</li>
 *     <li>Fallback default values for missing JSON fields to prevent NullPointerExceptions.</li>
 * </ul>
 */
public class JsonToPlaywrightMapper {
    private static final Gson gson = new Gson();

    /**
     * Maps a JSON object to a Playwright interface by dynamically creating a proxy implementation.
     *
     * @param <T> The Playwright interface type (e.g., {@code ConsoleMessage}, {@code Response}).
     * @param json The JSON object containing the data.
     * @param interfaceType The Playwright interface class.
     * @return A dynamically generated implementation of the requested interface.
     * @throws IllegalArgumentException if the JSON object is null.
     */
    @SuppressWarnings("unchecked")
    public static <T> T mapToInterface(JsonObject json, Class<T> interfaceType) {
        if (json == null) {
            throw new IllegalArgumentException("Received null JSON for mapping");
        }

        // Check if the JSON data is inside a "result" object
        JsonObject result = json.has("result") ? json.getAsJsonObject("result") : json;

        // Dynamically create a proxy instance for the interface
        return (T) Proxy.newProxyInstance(
                interfaceType.getClassLoader(),
                new Class<?>[]{interfaceType},
                new JsonInvocationHandler(result, interfaceType)
        );
    }

    /**
     * Invocation handler that intercepts method calls on a dynamically generated Playwright interface.
     * <p>
     * When a method is called, the handler attempts to fetch the corresponding field from the JSON object.
     * If the field is missing, a default value is returned.
     * </p>
     */
    private static class JsonInvocationHandler implements InvocationHandler {
        private final JsonObject json;
        private final Class<?> interfaceType;

        public JsonInvocationHandler(JsonObject json, Class<?> interfaceType) {
            this.json = json;
            this.interfaceType = interfaceType;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String methodName = method.getName();
            String fieldName = deriveFieldName(methodName);

            System.out.println("[DEBUG] Method call: " + methodName + " → JSON field: " + fieldName);

            if (json.has(fieldName) && !json.get(fieldName).isJsonNull()) {
                // Falls der Rückgabewert eine Liste von JSHandle ist
                if (method.getReturnType().equals(List.class)) {
                    Type listType = method.getGenericReturnType();
                    if (listType.getTypeName().contains("JSHandle")) {
                        return mapToJsHandleList(json.getAsJsonArray(fieldName));
                    }
                }
                return gson.fromJson(json.get(fieldName), method.getReturnType());
            }

            // 🛠 Spezialfall für Response-Objekte (Tiefere JSON-Struktur durchsuchen)
            if (json.has("response") && json.get("response").isJsonObject()) {
                JsonObject responseJson = json.getAsJsonObject("response");
                if (responseJson.has(fieldName) && !responseJson.get(fieldName).isJsonNull()) {
                    return gson.fromJson(responseJson.get(fieldName), method.getReturnType());
                }
            }

            return getDefaultReturnValue(method.getReturnType());
        }


        /**
         * Leitet den Feldnamen aus dem Methodennamen ab:
         * - "getXyz" → "xyz"
         * - "isXyz" → "xyz"
         * - "xyz" bleibt unverändert
         */
        private String deriveFieldName(String methodName) {
            if (methodName.startsWith("get") && methodName.length() > 3) {
                return Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
            } else if (methodName.startsWith("is") && methodName.length() > 2) {
                return Character.toLowerCase(methodName.charAt(2)) + methodName.substring(3);
            }
            return methodName;
        }

        /**
         * Gibt einen sinnvollen Standardwert für primitive Typen zurück.
         */
        private Object getDefaultReturnValue(Class<?> returnType) {
            if (returnType == String.class) {
                return "";  // 🔹 Sicherstellen, dass String nicht null, sondern "" zurückgibt!
            } else if (returnType == int.class || returnType == Integer.class) {
                return 0;
            } else if (returnType == boolean.class || returnType == Boolean.class) {
                return false;
            } else if (returnType == Map.class) {
                return Collections.emptyMap();
            } else if (returnType == List.class) {
                return Collections.emptyList();
            } else if (returnType.isInterface()) {
                return null;
            }
            return null;
        }
    }


    /**
     * Maps a JSON array to a list of dynamically generated {@code JSHandle} objects.
     *
     * @param jsonArray The JSON array representing a list of JavaScript handles.
     * @return A list of proxy-generated {@code JSHandle} objects.
     */
    private static List<JSHandle> mapToJsHandleList(JsonArray jsonArray) {
        if (jsonArray == null || jsonArray.size() == 0) {
            return Collections.emptyList();
        }

        List<JSHandle> jsHandles = new ArrayList<>();
        jsonArray.forEach(element -> {
            JSHandle jsHandle = (JSHandle) Proxy.newProxyInstance(
                    JSHandle.class.getClassLoader(),
                    new Class<?>[]{JSHandle.class},
                    new JsonInvocationHandler(element.getAsJsonObject(), JSHandle.class)
            );
            jsHandles.add(jsHandle);
        });

        return jsHandles;
    }

    /**
     * Returns a default value based on the method's return type.
     *
     * @param returnType The return type of the method.
     * @return A default value (e.g., empty string for {@code String}, {@code 0} for integers).
     */
    private static Object getDefaultReturnValue(Class<?> returnType) {
        if (returnType == String.class) {
            return "";
        } else if (returnType == int.class || returnType == Integer.class) {
            return 0;
        } else if (returnType == boolean.class || returnType == Boolean.class) {
            return false;
        } else if (returnType == Map.class) {
            return Collections.emptyMap();
        } else if (returnType == List.class) {
            return Collections.emptyList();
        } else if (returnType.isInterface()) {
            return null;
        }
        return null;
    }
}
