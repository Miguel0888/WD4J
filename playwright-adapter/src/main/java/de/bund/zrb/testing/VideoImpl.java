package de.bund.zrb.testing;

import com.microsoft.Video;

import java.nio.file.Path;

/**
 * NOT IMPLEMENTED YET
 *
 * Represents a video recording of a browser tab.
 *
 * @since v1.8
 */
// ToDo: Maybe implemented via a separate WebSocketServer (see WebSocketServer) Callback, which records the video data
//  sent by the browser. Can a a JavaScriptPreLoad script be used to record the video data?
public class VideoImpl implements Video {
    /**
     * Deletes the video file. Will wait for the video to finish if necessary.
     *
     * @since v1.11
     */
    @Override
    public void delete() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Returns the file system path this video will be recorded to. The video is guaranteed to be written to the filesystem
     * upon closing the browser context. This method throws when connected remotely.
     *
     * @since v1.8
     */
    @Override
    public Path path() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Saves the video to a user-specified path. It is safe to call this method while the video is still in progress, or after
     * the page has closed. This method waits until the page is closed and the video is fully saved.
     *
     * @param path Path where the video should be saved.
     * @since v1.11
     */
    @Override
    public void saveAs(Path path) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
