/*
 * Copyright (c) Microsoft Corporation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.microsoft.playwright;


/**
 * The Touchscreen class operates in main-frame CSS pixels relative to the top-left corner of the viewport. Methods on the
 * touchscreen can only be used in browser contexts that have been initialized with {@code hasTouch} set to true.
 */
public interface Touchscreen {
  /**
   * Dispatches a {@code touchstart} and {@code touchend} event with a single touch at the position ({@code x},{@code y}).
   *
   * <p> <strong>NOTE:</strong> {@link Page#tap Page.tap()} the method will throw if {@code hasTouch} option of the browser
   * context is false.
   *
   * @param x X coordinate relative to the main frame's viewport in CSS pixels.
   * @param y Y coordinate relative to the main frame's viewport in CSS pixels.
   * @since v1.8
   */
  void tap(double x, double y);
}

