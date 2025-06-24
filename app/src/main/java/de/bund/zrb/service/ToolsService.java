package de.bund.zrb.service;

public interface ToolsService {
    byte[] takeScreenshot();
    void testSelector(String selector, String action, boolean active);
}
