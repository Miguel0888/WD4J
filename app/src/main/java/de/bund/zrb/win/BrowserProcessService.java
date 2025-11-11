package de.bund.zrb.win;

public interface BrowserProcessService {
    BrowserInstanceState detectBrowserInstanceState(String executablePath);
    BrowserTerminationResult terminateBrowserInstances(String executablePath);
}

