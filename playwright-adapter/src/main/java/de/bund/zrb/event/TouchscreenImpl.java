package de.bund.zrb.event;

import com.microsoft.Touchscreen;

public class TouchscreenImpl implements Touchscreen {

    /**
     * Dispatches a {@code touchstart} and {@code touchend} event with a single touch at the position ({@code x},{@code y}).
     *
     * <p> <strong>NOTE:</strong> {@link PageImpl#tap Page.tap()} the method will throw if {@code hasTouch} option of the browser
     * context is false.
     *
     * @param x X coordinate relative to the main frame's viewport in CSS pixels.
     * @param y Y coordinate relative to the main frame's viewport in CSS pixels.
     * @since v1.8
     */
    @Override
    public void tap(double x, double y) {

    }
}
