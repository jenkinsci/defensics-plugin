/*
 * Copyright 2024 Black Duck Software, Inc. All rights reserved.
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

package com.defensics.jenkins;

import hudson.EnvVars;
import hudson.model.TaskListener;
import java.io.IOException;

public class PollingIntervals {

  private static final int TESTPLAN_LOADING_SLEEP_SECONDS = 5;
  private static final int INITIAL_RUN_POLLING_INTERVAL_SECONDS = 5;
  private static final int RUN_POLLING_INTERVAL_WHILE_RUNNING = 30;
  private int maxPollingInterval = -1;

  /**
   * Creates new instance of polling intervals provider. Gets environment variable
   * "DEFENSICS_MAX_POLLING_INTERVAL" from jenkinsRun and task listener, which is used to cap
   * polling intervals to make integration tests run faster. The variable could be used for making
   * Defensics plugin print run status information more often too.
   *
   * @param jenkinsRun The Jenkins run for which to check environment variables.
   * @param listener   The task listener for which to check environment variables.
   * @param logger     Logger for logging invalid environment variable values.
   */
  public PollingIntervals(hudson.model.Run<?, ?> jenkinsRun, TaskListener listener, Logger logger) {
    String environmentVariable = null;
    try {
      final EnvVars env = jenkinsRun.getEnvironment(listener);
      environmentVariable = env.get("DEFENSICS_MAX_POLLING_INTERVAL");
      if (environmentVariable != null) {
        maxPollingInterval = Integer.parseInt(environmentVariable);
      }
    } catch (NumberFormatException e) {
      // Environment variable is set, but the value is not a valid integer.
      logger.logError("Unable to parse DEFENSICS_MAX_POLLING_INTERVAL environment variable. "
          + environmentVariable + " is not a valid integer value.");
    } catch (InterruptedException | IOException e) {
      // Because getting the environment variable is not core functionality for the plugin, we will
      // let getting maximum polling interval fail and try to continue with fuzzing. If there is
      // something weird going on, the next step will fail anyway.
    }
  }

  public int getTestplanLoadingInterval() {
    return getIntervalOrMaximum(TESTPLAN_LOADING_SLEEP_SECONDS);
  }

  public int getInitialRunPollingInterval() {
    return getIntervalOrMaximum(INITIAL_RUN_POLLING_INTERVAL_SECONDS);
  }

  public int getRunPollingInterval() {
    return getIntervalOrMaximum(RUN_POLLING_INTERVAL_WHILE_RUNNING);
  }

  /**
   * Get correct polling interval.
   *
   * @param interval The "standard" interval to check against the environment variable configured
   *                 maximum polling interval.
   * @return The given interval, or, if a maximum interval is set and it is smaller than the given
   *     interval, return maximum interval.
   */
  private int getIntervalOrMaximum(int interval) {
    if (maxPollingInterval >= 0 && maxPollingInterval < interval) {
      return maxPollingInterval;
    } else {
      return interval;
    }
  }
}
