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

package com.defensics.apiserver.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Base class for RunTestConfiguration (run of a specific on-going run), ResultTestConfiguration
 * (Info about configuration that was used on some past run) and TestConfiguration (shared config
 * that can be used as basis for starting different runs).
 */
public abstract class BaseTestConfiguration {

  @JsonIgnore
  @Schema(description = "Human-readable name of the TestConfiguration",
      example = "Example configuration")
  protected String name;

  @JsonIgnore
  @Schema(description = "Id of the project this configuration belongs to")
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

  public String getProjectId() {
    return projectId;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setProjectId(String projectId) {
    this.projectId = projectId;
  }
}
