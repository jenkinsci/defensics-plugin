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

package com.defensics.jenkins.test.utils;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

/**
 * Utility methods to handle Jenkins jobs in tests, eg. interrupt it at the given time.
 */
public class JenkinsJobUtils {

  /**
   * Watches build log and trigger job interrupt when logString occurs in logs.
   *
   * @param lastBuild Build to watch
   * @param logString Regular expression string to look for in lines
   */
  public static void triggerAbortOnLogLine(
      WorkflowRun lastBuild,
      String logString
  ) {
    triggerAbortOnLogLine(lastBuild, logString, 1);
  }

  /**
   * Watches build log and trigger job interrupt if given logString has occurred
   * requiredOccurenceCount times.
   *
   * @param lastBuild Build to watch
   * @param logRegex Regular expression string to look for in the log lines
   * @param requiredOccurenceCount How many lines should have this to cause job interrupt
   */
  public static void triggerAbortOnLogLine(
      WorkflowRun lastBuild,
      String logRegex,
      int requiredOccurenceCount
  ) {
    final Pattern regex = Pattern.compile(logRegex);
    Executors.newSingleThreadExecutor().submit(() -> {
          try {
            while (true) {
              final boolean containsLogStrings = lastBuild.getLog(999)
                  .stream()
                  .filter(line -> regex.matcher(line).find())
                  .count() == requiredOccurenceCount;

              if (containsLogStrings) {
                System.out.println("===");
                System.out.println("Found line, aborting");
                // Use Jenkins' own Stop request instead of cancelling future
                lastBuild.doStop();
                //runFuture.cancel(true);
                return;
              }
              // Busy loop, replace this triggering with better one if found. Two problems:
              // 1) Doesn't allow precise abort on given step
              // 2) Extraneous log polling
              Thread.sleep(50);
            }
          } catch (InterruptedException | IOException e) {
            e.printStackTrace();
          }
        }
    );
  }
}
