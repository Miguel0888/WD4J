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

package wd4j.api.options;

import wd4j.api.BrowserContext;
import wd4j.api.Frame;
import wd4j.api.Page;

public interface BindingCallback {
  interface Source {
    BrowserContext context();
    Page page();
    Frame frame();
  }

  Object call(Source source, Object... args);
}
