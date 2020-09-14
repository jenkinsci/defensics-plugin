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
import io.crnk.core.resource.annotations.JsonApiField;
import io.crnk.core.resource.annotations.JsonApiRelation;
import io.crnk.core.resource.annotations.JsonApiRelationId;
import io.crnk.core.resource.annotations.JsonApiResource;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.File;
import java.time.ZonedDateTime;
import java.util.List;

@JsonApiResource(type = "runs")
public class Run extends BaseTestRun {

  @Schema(description = "Number of test cases that are going to be executed on the run")
  @JsonApiField(postable = false, patchable = false)
  private int casesToBeExecuted;

  // Case index changes during the run. Client can't specify these, so it's not postable/patchable.
  @Schema(description = "Index of the case run is currently executing")
  @JsonApiField(postable = false, patchable = false)
  private int caseIndex;

  @Schema(description = "Number of cases already executed on run")
  @JsonApiField(postable = false, patchable = false)
  private int runIndex;

  @JsonApiField(postable = false, patchable = false)
  private RunState state;

  @JsonApiField(postable = false, patchable = false)
  private RunVerdict verdict;

  // Actual run configuration containing the settings used by run. Auto-created
  // at run creation based on the shared configuration provided
  // Can't be changed by user (contents itself can be modified)
  @JsonApiRelation(mappedBy = "run")
  @JsonApiField(postable = false, patchable = false)
  protected RunTestConfiguration configuration;

  @JsonApiRelation(mappedBy = "run")
  @JsonApiField(postable = false, patchable = false)
  private List<FailureSummaryEntry> failureSummary;

  @JsonApiRelation
  @Schema(description = "Result corresponding this run")
  private Result result;

  @JsonApiRelationId
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
      ZonedDateTime startTime,
      String parentConfigurationId,
      int casesToBeExecuted,
      int caseIndex,
      int runIndex,
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

  public String getId() {
    return id;
  }

  public RunState getState() {
    return state;
  }

  public String getProjectId() {
    return projectId;
  }

  public RunVerdict getVerdict() {
    return verdict;
  }

  public RunTestConfiguration getConfiguration() {
    return configuration;
  }

  public Result getResult() {
    return result;
  }

  public String getResultId() {
    return resultId;
  }

  public void setFailureSummary(List<FailureSummaryEntry> failureSummary) {
    this.failureSummary = failureSummary;
  }

  public void setRunIndex(int runIndex) {
    this.runIndex = runIndex;
  }

  public void setCaseIndex(int caseIndex) {
    this.caseIndex = caseIndex;
  }

  public int getCaseIndex() {
    return caseIndex;
  }

  public int getRunIndex() {
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

  public int getCasesToBeExecuted() {
    return casesToBeExecuted;
  }

  public void setCasesToBeExecuted(int casesToBeExecuted) {
    this.casesToBeExecuted = casesToBeExecuted;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setProjectId(String projectId) {
    this.projectId = projectId;
  }

  public void setConfiguration(RunTestConfiguration configuration) {
    this.configuration = configuration;
  }

  public void setResult(Result result) {
    this.result = result;
  }

  public void setResultId(String resultId) {
    this.resultId = resultId;
  }
}
