/*
 * Copyright © 2020 Synopsys, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.synopsys.defensics.jenkins;

import hudson.model.TaskListener;

/**
 * A class for logging messages from Defensics plugin to Jenkins console.
 */
public class Logger {

  private final TaskListener listener;

  public Logger(TaskListener listener) {
    this.listener = listener;
  }

  public TaskListener getListener() {
    return listener;
  }

  public void println(String message) {
    listener.getLogger().println("[Defensics] " + message);
  }

  public void logError(String message) {
    println("[ERROR] " + message);
  }
}
