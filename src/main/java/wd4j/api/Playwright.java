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

package wd4j.api;

import java.util.*;

/**
 * Playwright module provides a method to launch a browser instance. The following is a typical example of using Playwright
 * to drive automation:
 * <pre>{@code
 * import com.microsoft.playwright.*;
 *
 * public class Example {
 *   public static void main(String[] args) {
 *     try (Playwright playwright = Playwright.create()) {
 *       BrowserType chromium = playwright.chromium();
 *       Browser browser = chromium.launch();
 *       Page page = browser.newPage();
 *       page.navigate("http://example.com");
 *       // other actions...
 *       browser.close();
 *     }
 *   }
 * }
 * }</pre>
 */
public interface Playwright extends AutoCloseable {
  class CreateOptions {
    /**
     * Additional environment variables that will be passed to the driver process. By default driver process inherits
     * environment variables of the Playwright process.
     */
    public Map<String, String> env;

    /**
     * Additional environment variables that will be passed to the driver process. By default driver process inherits
     * environment variables of the Playwright process.
     */
    public CreateOptions setEnv(Map<String, String> env) {
      this.env = env;
      return this;
    }
  }
  /**
   * This object can be used to launch or connect to Chromium, returning instances of {@code Browser}.
   *
   * @since v1.8
   */
  BrowserType chromium();
  /**
   * This object can be used to launch or connect to Firefox, returning instances of {@code Browser}.
   *
   * @since v1.8
   */
  BrowserType firefox();
  /**
   * Exposes API that can be used for the Web API testing.
   *
   * @since v1.16
   */
  APIRequest request();
  /**
   * Selectors can be used to install custom selector engines. See <a
   * href="https://playwright.dev/java/docs/extensibility">extensibility</a> for more information.
   *
   * @since v1.8
   */
  Selectors selectors();
  /**
   * This object can be used to launch or connect to WebKit, returning instances of {@code Browser}.
   *
   * @since v1.8
   */
  BrowserType webkit();
  /**
   * Terminates this instance of Playwright, will also close all created browsers if they are still running.
   *
   * @since v1.9
   */
  void close();
  /**
   * Launches new Playwright driver process and connects to it. {@link Playwright#close
   * Playwright.close()} should be called when the instance is no longer needed.
   * <pre>{@code
   * Playwright playwright = Playwright.create();
   * Browser browser = playwright.webkit().launch();
   * Page page = browser.newPage();
   * page.navigate("https://www.w3.org/");
   * playwright.close();
   * }</pre>
   *
   * @since v1.10
   */
  static Playwright create(CreateOptions options) {
    try {
        // Vollqualifizierten Klassennamen der Implementierung angeben
//        String implementationClassName = "com.microsoft.playwright.impl.PlaywrightImpl";
        String implementationClassName = "wd4j.impl.playwright.PlaywrightImpl";

        // Klasse laden
        Class<?> clazz = Class.forName(implementationClassName);

        // Methode `create(CreateOptions)` finden
        return (Playwright) clazz.getMethod("create", CreateOptions.class).invoke(null, options);
    } catch (ClassNotFoundException e) {
        throw new RuntimeException("Implementation class not found: " + e.getMessage(), e);
    } catch (NoSuchMethodException e) {
        throw new RuntimeException("Static method 'create(CreateOptions)' not found in class: " + e.getMessage(), e);
    } catch (Exception e) {
        throw new RuntimeException("Failed to invoke static create(CreateOptions) method", e);
    }
}

  static Playwright create() {
    return create(null);
  }
}

