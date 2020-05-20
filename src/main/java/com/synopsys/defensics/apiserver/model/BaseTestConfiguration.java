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

import io.crnk.core.resource.annotations.JsonApiRelation;
import io.crnk.core.resource.annotations.JsonApiRelationId;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Base class for RunTestConfiguration (run of a specific run) and TestConfiguration (shared config
 * that can be used as basis for starting different runs).
 */
public abstract class BaseTestConfiguration {

  @Schema(description = "Human-readable name of the TestConfiguration",
      example = "Example configuration")
  protected String name;

  @Schema(description = "Project this configuration belongs to")
  @JsonApiRelation
  protected Project project;

  @JsonApiRelationId
  protected String projectId;

  /**
   * Constructor for BaseTestConfiguration.
   */
  public BaseTestConfiguration() {
  }

  /**
   * Constructor for BaseTestConfiguration with some initial values provided.
   *
   * @param name Human-readable name of the TestConfiguration
   * @param projectId Project id of test configuration.
   */
  public BaseTestConfiguration(String name, String projectId) {
    this.name = name;
    this.projectId = projectId;
  }

  public String getName() {
    return name;
  }

  public Project getProject() {
    return project;
  }

  public String getProjectId() {
    return projectId;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setProject(Project project) {
    this.project = project;
  }

  public void setProjectId(String projectId) {
    this.projectId = projectId;
  }

  /**
   * As various configurations have different id schemes, sequence relation can't be specified on
   * the base class, instead we'll just specify getter and setter here.
   * @return Sequence of this test configuration.
   */
  abstract Sequence getSequence();

  /**
   * As various configurations have different id schemes, sequence relation can't be specified on
   * the base class, instead we'll just specify getter and setter here.
   * @param sequence Sequence of this test configuration.
   */
  abstract void setSequence(Sequence sequence);
}
