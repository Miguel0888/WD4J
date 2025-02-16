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
        private String method = WDMethodEvent.MESSAGE.getName();

        @Override
        public String getMethod() {
            return method;
        }

        public static class MessageParameters {
            private WDChannel WDChannel;
            private WDRemoteValue data;
            private WDSource WDSource;

            public MessageParameters(WDChannel WDChannel, WDRemoteValue data, WDSource WDSource) {
                this.WDChannel = WDChannel;
                this.data = data;
                this.WDSource = WDSource;
            }

            public WDChannel getChannel() {
                return WDChannel;
            }

            public void setChannel(WDChannel WDChannel) {
                this.WDChannel = WDChannel;
            }

            public WDRemoteValue getData() {
                return data;
            }

            public void setData(WDRemoteValue data) {
                this.data = data;
            }

            public WDSource getSource() {
                return WDSource;
            }

            public void setSource(WDSource WDSource) {
                this.WDSource = WDSource;
            }

            @Override
            public String toString() {
                return "MessageParameters{" +
                        "channel=" + WDChannel +
                        ", data=" + data +
                        ", source=" + WDSource +
                        '}';
            }
        }
    }

    public static class RealmCreated extends WDEvent<RealmCreated.RealmCreatedParameters> {
        private String method = WDMethodEvent.REALM_CREATED.getName();

        @Override
        public String getMethod() {
            return method;
        }

        public static class RealmCreatedParameters {
            private WDRealm WDRealm;

            public RealmCreatedParameters(WDRealm WDRealm) {
                this.WDRealm = WDRealm;
            }

            public WDRealm getRealm() {
                return WDRealm;
            }

            public void setRealm(WDRealm WDRealm) {
                this.WDRealm = WDRealm;
            }

            @Override
            public String toString() {
                return "RealmCreatedParameters{" +
                        "realm=" + WDRealm +
                        '}';
            }
        }
    }

    public static class RealmDestroyed extends WDEvent<RealmDestroyed.RealmDestroyedParameters> {
        private String method = WDMethodEvent.REALM_DESTROYED.getName();

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