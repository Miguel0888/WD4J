package wd4j.impl.webdriver.event;

import wd4j.impl.markerInterfaces.WDModule;
import wd4j.impl.webdriver.type.script.WDChannel;
import wd4j.impl.webdriver.type.script.WDRealm;
import wd4j.impl.webdriver.type.script.WDRemoteValue;
import wd4j.impl.webdriver.type.script.WDSource;
import wd4j.impl.websocket.WDEvent;

public class WDScriptEvent implements WDModule {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Event Types (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class WebSocketMessage extends WDEvent<WebSocketMessage.MessageParameters> {
        private String method = WDEventMapping.MESSAGE.getName();

        @Override
        public String getMethod() {
            return method;
        }

        public static class MessageParameters {
            private WDChannel channel;
            private WDRemoteValue data;
            private WDSource source;

            public MessageParameters(WDChannel channel, WDRemoteValue data, WDSource source) {
                this.channel = channel;
                this.data = data;
                this.source = source;
            }

            public WDChannel getChannel() {
                return channel;
            }

            public void setChannel(WDChannel channel) {
                this.channel = channel;
            }

            public WDRemoteValue getData() {
                return data;
            }

            public void setData(WDRemoteValue data) {
                this.data = data;
            }

            public WDSource getSource() {
                return source;
            }

            public void setSource(WDSource source) {
                this.source = source;
            }

            @Override
            public String toString() {
                return "MessageParameters{" +
                        "channel=" + channel +
                        ", data=" + data +
                        ", source=" + source +
                        '}';
            }
        }
    }

    public static class RealmCreated extends WDEvent<RealmCreated.RealmCreatedParameters> {
        private String method = WDEventMapping.REALM_CREATED.getName();

        @Override
        public String getMethod() {
            return method;
        }

        public static class RealmCreatedParameters {
            private WDRealm realm;

            public RealmCreatedParameters(WDRealm realm) {
                this.realm = realm;
            }

            public WDRealm getRealm() {
                return realm;
            }

            public void setRealm(WDRealm realm) {
                this.realm = realm;
            }

            @Override
            public String toString() {
                return "RealmCreatedParameters{" +
                        "realm=" + realm +
                        '}';
            }
        }
    }

    public static class RealmDestroyed extends WDEvent<RealmDestroyed.RealmDestroyedParameters> {
        private String method = WDEventMapping.REALM_DESTROYED.getName();

        @Override
        public String getMethod() {
            return method;
        }

        public static class RealmDestroyedParameters {
            private String realm;

            public RealmDestroyedParameters(String realm) {
                this.realm = realm;
            }

            public String getRealm() {
                return realm;
            }

            public void setRealm(String realm) {
                this.realm = realm;
            }

            @Override
            public String toString() {
                return "RealmDestroyedParameters{" +
                        "realm='" + realm + '\'' +
                        '}';
            }
        }
    }

}