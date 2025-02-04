package wd4j.impl.module.event;

import wd4j.impl.module.generic.Module;
import wd4j.impl.module.type.ScriptChannel;
import wd4j.impl.module.type.ScriptRealm;
import wd4j.impl.module.type.ScriptRemoteValue;
import wd4j.impl.module.type.ScriptSource;
import wd4j.impl.module.websocket.Event;

public class Script implements Module {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Event Types (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class Message extends Event<Message.MessageParameters> {
        private String method = Method.MESSAGE.getName();

        @Override
        public String getMethod() {
            return method;
        }

        public static class MessageParameters {
            private ScriptChannel channel;
            private ScriptRemoteValue data;
            private ScriptSource source;

            public MessageParameters(ScriptChannel channel, ScriptRemoteValue data, ScriptSource source) {
                this.channel = channel;
                this.data = data;
                this.source = source;
            }

            public ScriptChannel getChannel() {
                return channel;
            }

            public void setChannel(ScriptChannel channel) {
                this.channel = channel;
            }

            public ScriptRemoteValue getData() {
                return data;
            }

            public void setData(ScriptRemoteValue data) {
                this.data = data;
            }

            public ScriptSource getSource() {
                return source;
            }

            public void setSource(ScriptSource source) {
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
        private String method = Method.REALM_CREATED.getName();

        @Override
        public String getMethod() {
            return method;
        }

        public static class RealmCreatedParameters {
            private ScriptRealm realm;

            public RealmCreatedParameters(ScriptRealm realm) {
                this.realm = realm;
            }

            public ScriptRealm getRealm() {
                return realm;
            }

            public void setRealm(ScriptRealm realm) {
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
        private String method = Method.REALM_DESTROYED.getName();

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