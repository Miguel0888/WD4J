package wd4j.impl.playwright.event;

import wd4j.api.Download;
import wd4j.api.Page;
import wd4j.impl.webdriver.event.WDBrowsingContextEvent;

import java.io.InputStream;
import java.nio.file.Path;

public class DownloadImpl implements Download {

    public DownloadImpl(WDBrowsingContextEvent.DownloadWillBegin downloadWillBegin) {
        // TODO: Implement this
    }

    @Override
    public void cancel() {

    }

    @Override
    public InputStream createReadStream() {
        return null;
    }

    @Override
    public void delete() {

    }

    @Override
    public String failure() {
        return "";
    }

    @Override
    public Page page() {
        return null;
    }

    @Override
    public Path path() {
        return null;
    }

    @Override
    public void saveAs(Path path) {

    }

    @Override
    public String suggestedFilename() {
        return "";
    }

    @Override
    public String url() {
        return "";
    }
}
