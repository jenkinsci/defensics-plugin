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
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Test configuration model. Contains information about the test-configuration (e.g. name, settings,
 * assigned project). This is a shared configuration that can be used as basis when creating new
 * test runs.
 */
@JsonApiResource(type = "test-configurations")
public class TestConfiguration extends BaseTestConfiguration {
  @Schema(description = "Id of the TestConfiguration",
      example = "1669947d-0969-4da4-8d31-880fada73815")
  @JsonApiId
  private String id;

  @Schema(description = "Suite that is used on this configuration")
  @JsonApiRelation
  protected Suite suite;

  @Schema(description = "Sequence used in this configuration")
  @JsonApiRelation
  private Sequence sequence;

  @JsonApiRelationId
  private String sequenceId;

  /**
   * Constructor for TestConfiguration.
   */
  public TestConfiguration() {
  }

  /**
   * Constructor for TestConfiguration with initial values provided.
   *
   * @param id Test configuration id
   * @param name Test configuration name
   * @param projectId Project id
   * @param suite Suite
   */
  public TestConfiguration(String id, String name, String projectId, Suite suite) {
    this.id = id;
    this.name = name;
    this.projectId = projectId;
    this.suite = suite;
  }

  public TestConfiguration(String configurationId) {
    this.id = configurationId;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getProjectId() {
    return projectId;
  }

  public Suite getSuite() {
    return suite;
  }

  public void setId(String id) {
    this.id = id;
  }

  @Override
  Sequence getSequence() {
    return sequence;
  }

  @Override
  void setSequence(Sequence sequence) {
    this.sequence = sequence;
  }
}
