package wd4j.impl.webdriver.event;

import com.google.gson.JsonObject;
import wd4j.impl.markerInterfaces.WDModule;
import wd4j.impl.webdriver.type.script.WDChannel;
import wd4j.impl.webdriver.type.script.WDRealm;
import wd4j.impl.webdriver.type.script.WDRemoteValue;
import wd4j.impl.webdriver.type.script.WDSource;
import wd4j.impl.websocket.WDEvent;
import wd4j.impl.websocket.WDEventNames;

public class WDScriptEvent implements WDModule {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Event Types (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * The Response for an optional Channel Parameter. The Channel is identified by a unique ID.
     * Thus, Message (Parameters) are some kind of Callbacks. (maybe compared to stdout, stderr, etc.)
     *
     * @see WDChannel
     * @see wd4j.impl.webdriver.command.request.WDScriptRequest.CallFunction
     */
    public static class Message extends WDEvent<Message.MessageParameters> {
        private String method = WDEventNames.MESSAGE.getName();

        public Message(JsonObject json) {
            super(json, MessageParameters.class);
        }

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
        private String method = WDEventNames.REALM_CREATED.getName();

        public RealmCreated(JsonObject json) {
            super(json, RealmCreatedParameters.class);
        }

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
        private String method = WDEventNames.REALM_DESTROYED.getName();

        public RealmDestroyed(JsonObject json) {
            super(json, RealmDestroyedParameters.class);
        }

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