package de.bund.zrb.ui;

import de.bund.zrb.service.BrowserServiceImpl;
import de.bund.zrb.service.RecorderService;

import javax.swing.*;
import java.awt.*;
import java.util.UUID;

public class RecorderSession extends JPanel {

    private final String contextId;
    private final RecorderService recorderService;

    public RecorderSession(BrowserServiceImpl browserService) {
        super(new BorderLayout(8, 8));

        this.contextId = UUID.randomUUID().toString(); // 👉 Hier: später echten Context holen
        this.recorderService = new RecorderService();

        // Hier dein Panel, Buttons, Table usw.
    }

    public String getContextId() {
        return contextId;
    }

    public RecorderService getRecorderService() {
        return recorderService;
    }
}
