package wd4j.impl.webdriver.type.network;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;

import java.lang.reflect.Type;

@JsonAdapter(UrlPattern.UrlPatternAdapter.class) // 🔥 Automatische JSON-Konvertierung
public interface UrlPattern {
    String getType();

    // 🔥 **INNERE KLASSE für JSON-Deserialisierung**
    class UrlPatternAdapter implements JsonDeserializer<UrlPattern> {
        @Override
        public UrlPattern deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();

            if (!jsonObject.has("type")) {
                throw new JsonParseException("Missing 'type' field in UrlPattern JSON");
            }

            String type = jsonObject.get("type").getAsString();

            switch (type) {
                case "string":
                    return context.deserialize(jsonObject, UrlPatternString.class);
                case "pattern":
                    return context.deserialize(jsonObject, UrlPatternPattern.class);
                default:
                    throw new JsonParseException("Unknown UrlPattern type: " + type);
            }
        }
    }

    class UrlPatternString implements UrlPattern {
        private final String type = "string";
        private final String pattern;

        public UrlPatternString(String pattern) {
            this.pattern = pattern;
        }

        @Override
        public String getType() {
            return type;
        }

        public String getPattern() {
            return pattern;
        }
    }

    class UrlPatternPattern implements UrlPattern{
        private final String type = "pattern";
        private final String protocol; // optional
        private final String hostnames; // optional
        private final String port; // optional
        private final String pathname; // optional
        private final String search; // optional

        public UrlPatternPattern() {
            this.protocol = null;
            this.hostnames = null;
            this.port = null;
            this.pathname = null;
            this.search = null;
        }

        public UrlPatternPattern(String protocol, String hostnames, String port, String pathname, String search) {
            this.protocol = protocol;
            this.hostnames = hostnames;
            this.port = port;
            this.pathname = pathname;
            this.search = search;
        }

        @Override
        public String getType() {
            return type;
        }

        public String getProtocol() {
            return protocol;
        }

        public String getHostnames() {
            return hostnames;
        }

        public String getPort() {
            return port;
        }

        public String getPathname() {
            return pathname;
        }

        public String getSearch() {
            return search;
        }
    }
}