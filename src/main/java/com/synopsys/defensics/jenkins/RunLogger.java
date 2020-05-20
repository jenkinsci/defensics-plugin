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
import java.math.BigDecimal;

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
    final int totalCases = run.getCasesToBeExecuted();
    final int paddingSize = getNumberLength(totalCases);
    final String status = String
        .format("%4s%% (%" + paddingSize + "d/%d) of tests run. %s", getPercentage(run),
            run.getRunIndex(), totalCases, getFailureStatus(run));
    logger.println(status);
  }

  private String getPercentage(Run run) {
    final int runIndex = run.getRunIndex();
    final int total = run.getCasesToBeExecuted();
    final float percentage = runIndex * 100f / total;
    return BigDecimal.valueOf(percentage).setScale(1, BigDecimal.ROUND_HALF_UP).toPlainString();
  }

  private String getFailureStatus(Run run) {
    if (run.getCasesToBeExecuted() <= 0) {
      return "";
    }
    return DefensicsUtils.countRunFailures(run) > 0 ? "Some FAILED." : "All passed.";
  }

  /**
   * Returns count of digits in a number. Uses repeated multiplication which is faster than String
   * based solution or logarithmic approach.
   */
  private int getNumberLength(int number) {
    int length = 0;
    long temp = 1;
    while (temp <= number) {
      length++;
      temp *= 10;
    }
    return length;
  }
}
