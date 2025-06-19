package de.bund.zrb.type.session;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;

import java.lang.reflect.Type;
import java.util.List;

@JsonAdapter(WDProxyConfiguration.ProxyConfigurationAdapter.class)
public interface WDProxyConfiguration {
    String getProxyType();

    class ProxyConfigurationAdapter implements JsonDeserializer<WDProxyConfiguration> {
        @Override
        public WDProxyConfiguration deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();

        // 🔥 Falls `proxy` `{}` ist → `null` zurückgeben!
        if (jsonObject.entrySet().isEmpty()) {
            return null;
        }

        // 🔥 Sicherstellen, dass proxyType existiert!
        if (!jsonObject.has("proxyType") || jsonObject.get("proxyType").isJsonNull()) {
            throw new JsonParseException("Missing or null proxyType field in ProxyConfiguration JSON");
        }

            String proxyType = jsonObject.get("proxyType").getAsString();

            switch (proxyType) {
                case "autodetect":
                    return context.deserialize(jsonObject, AutodetectWDProxyConfiguration.class);
                case "direct":
                    return context.deserialize(jsonObject, DirectWDProxyConfiguration.class);
                case "manual":
                    return context.deserialize(jsonObject, ManualWDProxyConfiguration.class);
                case "pac":
                    return context.deserialize(jsonObject, PacWDProxyConfiguration.class);
                case "system":
                    return context.deserialize(jsonObject, SystemWDProxyConfiguration.class);
                default:
                    throw new JsonParseException("Unknown proxy type: " + proxyType);
            }
        }
    }

    class AutodetectWDProxyConfiguration implements WDProxyConfiguration {
        private final String proxyType = "autodetect";

        @Override
        public String getProxyType() {
            return proxyType;
        }
    }

    class DirectWDProxyConfiguration implements WDProxyConfiguration {
        private final String proxyType = "direct";

        @Override
        public String getProxyType() {
            return proxyType;
        }
    }

    class ManualWDProxyConfiguration implements WDProxyConfiguration {
        private final String proxyType = "manual";
        private final String ftpProxy; // Optional
        private final String httpProxy; // Optional
        private final String sslProxy; // Optional
        private final SocksProxyConfiguration socksProxyConfiguration; // Optional
        private final List<String> noProxy; // Optional

        public ManualWDProxyConfiguration(String ftpProxy, String httpProxy, String sslProxy,
                                          SocksProxyConfiguration socksProxyConfiguration, List<String> noProxy) {
            this.ftpProxy = ftpProxy;
            this.httpProxy = httpProxy;
            this.sslProxy = sslProxy;
            this.socksProxyConfiguration = socksProxyConfiguration;
            this.noProxy = noProxy;
        }

        @Override
        public String getProxyType() {
            return proxyType;
        }

        public String getFtpProxy() {
            return ftpProxy;
        }

        public String getHttpProxy() {
            return httpProxy;
        }

        public String getSslProxy() {
            return sslProxy;
        }

        public SocksProxyConfiguration getSocksProxyConfiguration() {
            return socksProxyConfiguration;
        }

        public List<String> getNoProxy() {
            return noProxy;
        }
    }

    class SocksProxyConfiguration {
        private final String socksProxy;
        private final byte socksVersion;

        public SocksProxyConfiguration(String socksProxy, byte socksVersion) {
            this.socksProxy = socksProxy;
            this.socksVersion = socksVersion;
        }

        public String getSocksProxy() {
            return socksProxy;
        }

        public int getSocksVersion() {
            return socksVersion;
        }
    }

    class PacWDProxyConfiguration implements WDProxyConfiguration {
        private final String proxyType = "pac";
        private final String proxyAutoconfigUrl;

        public PacWDProxyConfiguration(String proxyAutoconfigUrl) {
            this.proxyAutoconfigUrl = proxyAutoconfigUrl;
        }

        @Override
        public String getProxyType() {
            return proxyType;
        }

        public String getProxyAutoconfigUrl() {
            return proxyAutoconfigUrl;
        }
    }

    class SystemWDProxyConfiguration implements WDProxyConfiguration {
        private final String proxyType = "system";

        @Override
        public String getProxyType() {
            return proxyType;
        }
    }
}

