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

import io.crnk.core.resource.annotations.JsonApiField;
import io.crnk.core.resource.annotations.JsonApiId;
import io.crnk.core.resource.annotations.JsonApiRelation;
import io.crnk.core.resource.annotations.JsonApiRelationId;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.ZonedDateTime;

/**
 * Base class for Run (a test run that is currently running) and Result (a test run that has
 * finished, for which we only have data loaded from saved run info).
 */
public abstract class BaseTestRun {

  @JsonApiId
  @Schema(description = "Id of the Result", example = "1a21fb37-d173-41af-8a71-5bbe06249f7f")
  protected String id;

  @Schema(description = "Name of the Run", example = "20200305-1217-58")
  protected String runName;

  @Schema(description = "Project this run belongs")
  @JsonApiRelation
  protected Project project;

  @JsonApiRelationId
  protected String projectId;

  protected RunType runType;

  // Auto-generated field, not modifiable by user
  @JsonApiField(postable = false, patchable = false)
  protected ZonedDateTime runStartTime;

  // Reference to the shared configuration that was used to create the run
  // and used as reference for creating the actual run configuration.
  // Must be specified at creation, can't be changed after that
  @JsonApiRelation
  @JsonApiField(postable = true, patchable = false)
  protected TestConfiguration parentConfiguration;

  @JsonApiRelationId
  private String parentConfigurationId;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getRunName() {
    return runName;
  }

  public void setRunName(String runName) {
    this.runName = runName;
  }

  public TestConfiguration getParentConfiguration() {
    return parentConfiguration;
  }

  public void setParentConfiguration(
      TestConfiguration parentConfiguration) {
    this.parentConfiguration = parentConfiguration;
  }

  public String getParentConfigurationId() {
    return parentConfigurationId;
  }

  public void setParentConfigurationId(String parentConfigurationId) {
    this.parentConfigurationId = parentConfigurationId;
  }

  public Project getProject() {
    return project;
  }

  public void setProject(Project project) {
    this.project = project;
  }

  public String getProjectId() {
    return projectId;
  }

  public void setProjectId(String projectId) {
    this.projectId = projectId;
  }

  public RunType getRunType() {
    return runType;
  }

  public void setRunType(RunType runType) {
    this.runType = runType;
  }

  public ZonedDateTime getRunStartTime() {
    return runStartTime;
  }

  public void setRunStartTime(ZonedDateTime runStartTime) {
    this.runStartTime = runStartTime;
  }
}
