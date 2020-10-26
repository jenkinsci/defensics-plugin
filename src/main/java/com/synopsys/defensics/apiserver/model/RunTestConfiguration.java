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

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Configuration of single test run (as opposed to TestConfiguration that's a shared configuration
 * that can be used as a base for creating configurations to single runs.)
 */
public class RunTestConfiguration extends BaseTestConfiguration {

  @Schema(description = "Id of the run this configuration belongs to")
  private String runId;

  @Schema(description = "Id of the main suite instance this configuration uses")
  private String suiteInstanceId;

  /**
   * Suite could use multiple sequences (e.g. SIP has different "Registration sequence",
   * "Used Notify sequence", etc. sequences). Other sequences are usually in support role, or more
   * akin to settings. Use here the main sequence which will be shown in the UI as the primary
   * sequence.
   */
  @Schema(description = "Id of the sequence this configuration uses")
  private String sequenceId;

  /**
   * Parent configuration that was used as a base when this configuration (=run using this
   * configuration) was created.
   */
  @Schema(
      description = "Id of the shared configuration that was used as a basis for this configuration"
  )
  private String parentConfigurationId;


  /**
   * Constructor for RunTestConfiguration.
   */
  public RunTestConfiguration() {
  }

  /**
   * Constructor for RunTestConfiguration with some default values provided.
   * @param runId Run id
   * @param name Name of test configuration
   * @param projectId Project id
   * @param suiteInstanceId Suite instance id
   * @param parentConfigurationId Parent configuration id
   */
  public RunTestConfiguration(String runId, String name, String projectId,
      String sequenceId,
      String suiteInstanceId,
      String parentConfigurationId) {
    super(name, projectId);
    this.runId = runId;
    this.suiteInstanceId = suiteInstanceId;
    this.sequenceId = sequenceId;
    this.parentConfigurationId = parentConfigurationId;
  }

  public String getRunId() {
    return runId;
  }

  public void setRunId(String runId) {
    this.runId = runId;
  }


  public String getSuiteInstanceId() {
    return suiteInstanceId;
  }

  public void setSuiteInstanceId(String suiteInstanceId) {
    this.suiteInstanceId = suiteInstanceId;
  }

  public String getSequenceId() {
    return sequenceId;
  }

  public void setSequenceId(String sequenceId) {
    this.sequenceId = sequenceId;
  }

  public String getParentConfigurationId() {
    return parentConfigurationId;
  }

  public void setParentConfigurationId(String parentConfigurationId) {
    this.parentConfigurationId = parentConfigurationId;
  }
}
