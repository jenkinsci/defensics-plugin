/*
 * Copyright © 2020-2022 Synopsys, Inc.
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
import java.io.File;
import java.time.OffsetDateTime;
import java.util.List;

public class Run extends BaseTestRun {

  @Schema(description = "Number of test cases that are going to be executed on the test run")
  private long casesToBeExecuted;

  @Schema(description = "Number of test cases executed")
  private long testCasesExecuted;

  // Case index changes during the run. Client can't specify these, so it's not postable/patchable.
  @Schema(description = "Index of the test case run is currently executing")
  private Long caseIndex;

  @Schema(description = "Run-index of the test case run is currently executing")
  private Long runIndex;

  @Schema(description = "Test run state")
  private RunState state;

  @Schema(description = "Test run verdict")
  private RunVerdict verdict;

  // Actual run configuration containing the settings used by run. Auto-created
  // at run creation based on the shared configuration provided
  // Can't be changed by user (contents itself can be modified)
  @JsonIgnore
  protected RunTestConfiguration configuration;

  @Schema(
      description = "Contains instrumentation failure counts if query parameter "
          + "?include=failure-summary is given. Note: This field and the inclusion mechanism is "
          + "unstable and may change in coming releases."
  )
  private List<FailureSummaryEntry> failureSummary;

  @Schema(description = "Id of the result corresponding to this test run")
  private String resultId;

  /**
   * Directory where the results are written to. Not used for serializing so transient.
   */
  @JsonIgnore
  private transient File targetDirectory;

  public Run() {
  }

  /**
   * Constructor.
   * @param id Run id
   * @param name Name of the run
   * @param projectId Project id
   * @param runType type of the run
   * @param startTime start time of the run
   * @param parentConfigurationId Id of the parent configuration of this run
   * @param casesToBeExecuted How many cases are to be executed in this run.
   * @param caseIndex Case index.
   * @param runIndex Run index.
   * @param state Current state of run.
   * @param verdict Verdict for run.
   * @param failureSummary Failure summary.
   */
  public Run(
      String id,
      String name,
      String projectId,
      RunType runType,
      OffsetDateTime startTime,
      String parentConfigurationId,
      long casesToBeExecuted,
      long testCasesExecuted,
      Long caseIndex,
      Long runIndex,
      RunState state,
      RunVerdict verdict,
      List<FailureSummaryEntry> failureSummary,
      String resultId
  ) {
    super(
        id,
        name,
        projectId,
        runType,
        startTime,
        parentConfigurationId
    );
    this.id = id;
    this.casesToBeExecuted = casesToBeExecuted;
    this.testCasesExecuted = testCasesExecuted;
    this.caseIndex = caseIndex;
    this.runIndex = runIndex;
    this.state = state;
    this.verdict = verdict;
    this.failureSummary = failureSummary;
    this.resultId = resultId;
  }

  public Run(String runId) {
    this.id = runId;
  }

  public Run(String runId, String projectId) {
    this.id = runId;
    this.projectId = projectId;
  }

  @Override
  public String getId() {
    return id;
  }

  public RunState getState() {
    return state;
  }

  @Override
  public String getProjectId() {
    return projectId;
  }

  public RunVerdict getVerdict() {
    return verdict;
  }

  public RunTestConfiguration getConfiguration() {
    return configuration;
  }

  public String getResultId() {
    return resultId;
  }

  public void setFailureSummary(List<FailureSummaryEntry> failureSummary) {
    this.failureSummary = failureSummary;
  }

  public void setRunIndex(long runIndex) {
    this.runIndex = runIndex;
  }

  public void setCaseIndex(long caseIndex) {
    this.caseIndex = caseIndex;
  }

  public Long getCaseIndex() {
    return caseIndex;
  }

  public Long getRunIndex() {
    return runIndex;
  }

  public List<FailureSummaryEntry> getFailureSummary() {
    return failureSummary;
  }

  public void setVerdict(RunVerdict verdict) {
    this.verdict = verdict;
  }

  public void setState(RunState state) {
    this.state = state;
  }

  public File getTargetDirectory() {
    return targetDirectory;
  }

  public void setTargetDirectory(File targetDirectory) {
    this.targetDirectory = targetDirectory;
  }

  public long getCasesToBeExecuted() {
    return casesToBeExecuted;
  }

  public void setCasesToBeExecuted(long casesToBeExecuted) {
    this.casesToBeExecuted = casesToBeExecuted;
  }

  public long getTestCasesExecuted() {
    return testCasesExecuted;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  @Override
  public void setProjectId(String projectId) {
    this.projectId = projectId;
  }

  public void setConfiguration(RunTestConfiguration configuration) {
    this.configuration = configuration;
  }

  public void setResultId(String resultId) {
    this.resultId = resultId;
  }
}
