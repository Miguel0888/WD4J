package de.bund.zrb.event;

import com.google.gson.JsonObject;
import de.bund.zrb.api.markerInterfaces.WDModule;
import de.bund.zrb.type.input.WDFileDialogInfo;
import de.bund.zrb.websocket.WDEvent;
import de.bund.zrb.websocket.WDEventNames;

/**
 * Input module events.
 */
public class WDInputEvent implements WDModule {
    public WDInputEvent(JsonObject json) {
        // Intentionally empty
    }

    public static class FileDialogOpened extends WDEvent<WDFileDialogInfo> {
        private String method = WDEventNames.FILE_DIALOG_OPENED.getName();

        public FileDialogOpened(JsonObject json) {
            // NOTE: Json passed here must be the event 'params' object
            super(json, WDFileDialogInfo.class);
        }

        @Override
        public String getMethod() {
            return method;
        }
    }
}
