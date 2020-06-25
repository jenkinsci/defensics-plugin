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
    // The printstream used by logger doesn't print anything if thread interrupted flag
    // is on, resulting in cases where the cleanup part of the job is not logged even though
    // the job is still running.
    //
    // Workaround this by checking/clearing interrupted flag and resetting after print.
    // There's likely better ways to handle this, but not yet known. Change if this proves to be
    // problematic.
    final boolean interrupted = Thread.interrupted();
    try {
      listener.getLogger().println("[Defensics] " + message);
    } finally {
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }

  public void logError(String message) {
    println("[ERROR] " + message);
  }
}
