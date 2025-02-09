package wd4j.impl.webdriver.type.script;

public interface RemoteReference {
    String getType();

    class SharedReference implements RemoteReference {
        private final String sharedId;
        private final String handle; // Optional

        public SharedReference(String sharedId) {
            this(sharedId, null);
        }

        public SharedReference(String sharedId, String handle) {
            this.sharedId = sharedId;
            this.handle = handle;
        }

        @Override
        public String getType() {
            return "shared-reference";
        }

        public String getSharedId() {
            return sharedId;
        }

        public String getHandle() {
            return handle;
        }
    }

    class RemoteObjectReference implements RemoteReference {
        private final String handle;
        private final String sharedId; // Optional

        public RemoteObjectReference(String handle) {
            this(handle, null);
        }

        public RemoteObjectReference(String handle, String sharedId) {
            this.handle = handle;
            this.sharedId = sharedId;
        }

        @Override
        public String getType() {
            return "remote-object-reference";
        }

        public String getHandle() {
            return handle;
        }

        public String getSharedId() {
            return sharedId;
        }
    }
}
