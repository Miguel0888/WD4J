package wd4j.impl.webdriver.event;

import wd4j.impl.markerInterfaces.Module;
import wd4j.impl.webdriver.type.script.Channel;
import wd4j.impl.webdriver.type.script.Realm;
import wd4j.impl.webdriver.type.script.RemoteValue;
import wd4j.impl.webdriver.type.script.Source;
import wd4j.impl.websocket.Event;

public class ScriptEvent implements Module {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Event Types (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class Message extends Event<Message.MessageParameters> {
        private String method = MethodEvent.MESSAGE.getName();

        @Override
        public String getMethod() {
            return method;
        }

        public static class MessageParameters {
            private Channel channel;
            private RemoteValue data;
            private Source source;

            public MessageParameters(Channel channel, RemoteValue data, Source source) {
                this.channel = channel;
                this.data = data;
                this.source = source;
            }

            public Channel getChannel() {
                return channel;
            }

            public void setChannel(Channel channel) {
                this.channel = channel;
            }

            public RemoteValue getData() {
                return data;
            }

            public void setData(RemoteValue data) {
                this.data = data;
            }

            public Source getSource() {
                return source;
            }

            public void setSource(Source source) {
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

    public static class RealmCreated extends Event<RealmCreated.RealmCreatedParameters> {
        private String method = MethodEvent.REALM_CREATED.getName();

        @Override
        public String getMethod() {
            return method;
        }

        public static class RealmCreatedParameters {
            private Realm realm;

            public RealmCreatedParameters(Realm realm) {
                this.realm = realm;
            }

            public Realm getRealm() {
                return realm;
            }

            public void setRealm(Realm realm) {
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

    public static class RealmDestroyed extends Event<RealmDestroyed.RealmDestroyedParameters> {
        private String method = MethodEvent.REALM_DESTROYED.getName();

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