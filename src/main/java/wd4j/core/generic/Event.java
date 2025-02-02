package wd4j.core.generic;

import com.google.gson.JsonObject;

public interface Event {
    String getType();
    JsonObject getData();

    // ToDo: Check location of this class!
    class WebDriverEvent implements Event {
        private final String type;
        private final JsonObject data;

        public WebDriverEvent(String type, JsonObject data) {
            this.type = type;
            this.data = data;
        }

        @Override
        public String getType() {
            return type;
        }

        @Override
        public JsonObject getData() {
            return data;
        }
    }
}