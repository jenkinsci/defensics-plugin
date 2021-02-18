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

package com.synopsys.defensics.apiserver.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * Information about some past test run. As this describes data about past event, (nearly)
 * everything here should be read-only.
 */
public class Result extends BaseTestRun {

  @Schema(description = "Number of test cases executed", example = "42321")
  private Long testCasesExecuted;

  @Schema(
      description = "Percentage of planned run completed (null for unlimited runs)",
      example = "43.5"
  )
  private BigDecimal completionPercentage;

  @Schema(description = "How run execution ended (finished, not finished))", example = "FINISHED")
  private RunStoppingStatus stoppingStatus;

  @Schema(description = "Total verdict of the test run", example = "FAIL")
  private RunVerdict verdict;

  @Schema(description = "When test run ended")
  private OffsetDateTime runEndTime;

  @Schema(description = "Run duration in seconds", type = "number", example = "255.421")
  private Duration runDuration;

  @Schema(description = "Information about configuration that was used on the run")
  @JsonIgnore
  protected ResultTestConfiguration configuration;

  public Result() {
  }

  /**
   * Constructor.
   *
   * @param id Result ID
   * @param runName Run name
   * @param projectId Project ID
   * @param testCasesExecuted How many cases have been executed
   * @param completionPercentage Completion percentage (check field definition for details)
   * @param stoppingStatus Execution outcome of the run
   * @param runType Run type
   * @param verdict Run verdict
   * @param runStartTime Run starting time
   * @param runEndTime Run ending time
   * @param runDuration Run duration
   */
  public Result(String id, String runName, String projectId, Long testCasesExecuted,
      BigDecimal completionPercentage, RunStoppingStatus stoppingStatus, RunType runType,
      RunVerdict verdict, OffsetDateTime runStartTime, OffsetDateTime runEndTime,
      Duration runDuration) {
    super(id, runName, projectId, runType, runStartTime, null);
    this.testCasesExecuted = testCasesExecuted;
    this.completionPercentage = completionPercentage;
    this.stoppingStatus = stoppingStatus;
    this.verdict = verdict;
    this.runEndTime = runEndTime;
    this.runDuration = runDuration;
  }

  public Long getTestCasesExecuted() {
    return testCasesExecuted;
  }

  public void setTestCasesExecuted(Long testCasesExecuted) {
    this.testCasesExecuted = testCasesExecuted;
  }

  public BigDecimal getCompletionPercentage() {
    return completionPercentage;
  }

  public void setCompletionPercentage(BigDecimal completionPercentage) {
    this.completionPercentage = completionPercentage;
  }

  public RunStoppingStatus getStoppingStatus() {
    return stoppingStatus;
  }

  public void setStoppingStatus(RunStoppingStatus stoppingStatus) {
    this.stoppingStatus = stoppingStatus;
  }

  public RunVerdict getVerdict() {
    return verdict;
  }

  public void setVerdict(RunVerdict verdict) {
    this.verdict = verdict;
  }

  public OffsetDateTime getRunEndTime() {
    return runEndTime;
  }

  public void setRunEndTime(OffsetDateTime runEndTime) {
    this.runEndTime = runEndTime;
  }

  public Duration getRunDuration() {
    return runDuration;
  }

  public void setRunDuration(Duration runDuration) {
    this.runDuration = runDuration;
  }

  public ResultTestConfiguration getConfiguration() {
    return configuration;
  }

  public void setConfiguration(
      ResultTestConfiguration configuration) {
    this.configuration = configuration;
  }
}
