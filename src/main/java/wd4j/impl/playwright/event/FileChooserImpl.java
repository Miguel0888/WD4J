package wd4j.impl.playwright.event;

import wd4j.api.ElementHandle;
import wd4j.api.FileChooser;
import wd4j.api.Page;
import wd4j.api.options.FilePayload;

import java.nio.file.Path;

/**
 * NOT IMPLEMENTED YET
 */
public class FileChooserImpl implements FileChooser {

    private final Page page;

    public FileChooserImpl(Page page) {
        this.page = page;
    }

    /**
     * Returns input element associated with this file chooser.
     *
     * @since v1.8
     */
    @Override
    public ElementHandle element() {
        return null;
    }

    /**
     * Returns whether this file chooser accepts multiple files.
     *
     * @since v1.8
     */
    @Override
    public boolean isMultiple() {
        return false;
    }

    /**
     * Returns page this file chooser belongs to.
     *
     * @since v1.8
     */
    @Override
    public Page page() {
        return page;
    }

    /**
     * Sets the value of the file input this chooser is associated with. If some of the {@code filePaths} are relative paths,
     * then they are resolved relative to the current working directory. For empty array, clears the selected files.
     *
     * @param files
     * @param options
     * @since v1.8
     */
    @Override
    public void setFiles(Path files, SetFilesOptions options) {

    }

    /**
     * Sets the value of the file input this chooser is associated with. If some of the {@code filePaths} are relative paths,
     * then they are resolved relative to the current working directory. For empty array, clears the selected files.
     *
     * @param files
     * @param options
     * @since v1.8
     */
    @Override
    public void setFiles(Path[] files, SetFilesOptions options) {

    }

    /**
     * Sets the value of the file input this chooser is associated with. If some of the {@code filePaths} are relative paths,
     * then they are resolved relative to the current working directory. For empty array, clears the selected files.
     *
     * @param files
     * @param options
     * @since v1.8
     */
    @Override
    public void setFiles(FilePayload files, SetFilesOptions options) {

    }

    /**
     * Sets the value of the file input this chooser is associated with. If some of the {@code filePaths} are relative paths,
     * then they are resolved relative to the current working directory. For empty array, clears the selected files.
     *
     * @param files
     * @param options
     * @since v1.8
     */
    @Override
    public void setFiles(FilePayload[] files, SetFilesOptions options) {

    }
}
