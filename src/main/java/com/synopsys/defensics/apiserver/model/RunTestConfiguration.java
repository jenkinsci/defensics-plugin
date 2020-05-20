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

import io.crnk.core.resource.annotations.JsonApiId;
import io.crnk.core.resource.annotations.JsonApiRelation;
import io.crnk.core.resource.annotations.JsonApiRelationId;
import io.crnk.core.resource.annotations.JsonApiResource;
import io.crnk.core.resource.annotations.LookupIncludeBehavior;
import io.crnk.core.resource.annotations.RelationshipRepositoryBehavior;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Configuration of single test run (as opposed to TestConfiguration that's a shared configuration
 * that can be used as a base for creating configurations to single runs.)
 */
@JsonApiResource(type = "run-test-configurations", nested = true)
public class RunTestConfiguration extends BaseTestConfiguration {

  @JsonApiId
  @JsonApiRelationId
  private String runId;

  @JsonApiRelation(
      lookUp = LookupIncludeBehavior.AUTOMATICALLY_WHEN_NULL,
      repositoryBehavior = RelationshipRepositoryBehavior.FORWARD_OWNER
  )
  @Schema(description = "Run this configuration belongs to")
  private Run run;

  @JsonApiRelation
  @Schema(description = "Main suite instance this configuration uses")
  private SuiteInstance suiteInstance;

  @JsonApiRelationId
  private String suiteInstanceId;

  /**
   * Suite could use multiple sequences (e.g. SIP has different "Registration sequence",
   * "Used Notify sequence", etc. sequences). Other sequences are usually in support role, or more
   * akin to settings. Use here the main sequence which will be shown in the UI as the primary
   * sequence.
   */
  @Schema(description = "Sequence this configuration uses")
  @JsonApiRelation
  private Sequence sequence;

  @JsonApiRelationId
  private String sequenceId;

  /**
   * Parent configuration that was used as a base when this configuration (=run using this
   * configuration) was created.
   */
  @JsonApiRelation
  @Schema(description = "Shared configuration that was used as a basis for this configuration")
  private TestConfiguration parentConfiguration;

  @JsonApiRelationId
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

  public Run getRun() {
    return run;
  }

  public void setRun(Run run) {
    this.run = run;
  }

  public SuiteInstance getSuiteInstance() {
    return suiteInstance;
  }

  public void setSuiteInstance(SuiteInstance suiteInstance) {
    this.suiteInstance = suiteInstance;
  }

  public String getSuiteInstanceId() {
    return suiteInstanceId;
  }

  public void setSuiteInstanceId(String suiteInstanceId) {
    this.suiteInstanceId = suiteInstanceId;
  }

  @Override
  public Sequence getSequence() {
    return sequence;
  }

  @Override
  public void setSequence(Sequence sequence) {
    this.sequence = sequence;
  }

  public String getSequenceId() {
    return sequenceId;
  }

  public void setSequenceId(String sequenceId) {
    this.sequenceId = sequenceId;
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
}
