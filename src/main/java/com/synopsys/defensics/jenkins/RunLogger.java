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

import com.synopsys.defensics.apiserver.model.Run;
import com.synopsys.defensics.jenkins.util.DefensicsUtils;

public class RunLogger {

  private final Logger logger;

  public RunLogger(Logger logger) {
    this.logger = logger;
  }

  /**
   * Writes current test run status to Jenkins log.
   *
   * @param run {@code Run} object for reading the run status.
   */
  public void log(Run run) {
    final long totalCases = run.getCasesToBeExecuted();
    final int paddingSize = getNumberLength(totalCases);
    final String status = String.format(
        "%4.1f%% (%" + paddingSize + "d/%d) of tests run. %s",
        getPercentage(run),
        run.getRunIndex(),
        totalCases,
        getFailureStatus(run)
    );
    logger.println(status);
  }

  private float getPercentage(Run run) {
    final long runIndex = run.getRunIndex();
    final long total = run.getCasesToBeExecuted();
    if (total == 0) {
      // Return 100% in cases where zero cases meant to be run.
      return 100f;
    }
    return runIndex * 100f / total;
  }

  private String getFailureStatus(Run run) {
    if (run.getCasesToBeExecuted() <= 0) {
      return "";
    }
    return DefensicsUtils.countRunFailures(run) > 0 ? "Some FAILED." : "All passed.";
  }

  /**
   * Returns count of digits in a number.
   */
  private int getNumberLength(long number) {
    return Long.toString(number, 10).length();
  }
}
