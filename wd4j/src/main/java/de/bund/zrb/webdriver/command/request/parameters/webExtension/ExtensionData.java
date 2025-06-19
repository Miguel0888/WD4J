package de.bund.zrb.webdriver.command.request.parameters.webExtension;

public interface ExtensionData {

    String getType();

    class ExtensionArchivePath implements ExtensionData {
        private final String type = "archivePath";
        private final String path;

        public ExtensionArchivePath(String path) {
            this.path = path;
        }

        public String getType() {
            return type;
        }

        public String getPath() {
            return path;
        }
    }

    class ExtensionBase64Encoded implements ExtensionData {
        private final String type = "base64";
        private final String value;

        public ExtensionBase64Encoded(String value) {
            this.value = value;
        }

        public String getType() {
            return type;
        }

        public String getValue() {
            return value;
        }
    }

    class ExtensionPath implements ExtensionData {
        private final String type = "path";
        private final String path;

        public ExtensionPath(String path) {
            this.path = path;
        }

        public String getType() {
            return type;
        }

        public String getPath() {
            return path;
        }
    }
}
