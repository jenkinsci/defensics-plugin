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
import io.crnk.core.resource.annotations.JsonApiResource;

/**
 * Suite instance, e.g. loaded or running suite with id. The available and installed suites are
 * defined with {@link Suite}.
 */
@JsonApiResource(type = "suite-instances")
public class SuiteInstance {
  @JsonApiId
  private String id;

  // These are read-only status info
  @JsonApiField(postable = false, patchable = false)
  private SuiteRunState state;
  @JsonApiField(postable = false, patchable = false)
  private String error;

  // Must be specified on creation, can't be updated afterwards
  @JsonApiRelation
  @JsonApiField(postable = true, patchable = false)
  private Suite suite;

  @JsonApiRelationId
  @JsonApiField(postable = true, patchable = false)
  private String suiteId;

  @JsonApiRelation(mappedBy = "suiteInstance")
  private RunTestConfiguration runTestConfiguration;

  /**
   * Constructor for suite instance.
   */
  public SuiteInstance() {
  }

  /**
   * Constructor for suite instance with initial values provided.
   *
   * @param id Suite instance id
   * @param state Suite instance run state
   * @param error Error
   * @param suiteId Suite id
   */
  public SuiteInstance(
      String id,
      SuiteRunState state,
      String error,
      String suiteId) {
    this.id = id;
    this.state = state;
    this.error = error;
    this.suiteId = suiteId;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public SuiteRunState getState() {
    return state;
  }

  public void setState(SuiteRunState state) {
    this.state = state;
  }

  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }

  public Suite getSuite() {
    return suite;
  }

  public void setSuite(Suite suite) {
    this.suite = suite;
  }

  public String getSuiteId() {
    return suiteId;
  }

  public void setSuiteId(String suiteId) {
    this.suiteId = suiteId;
  }

  public RunTestConfiguration getRunTestConfiguration() {
    return runTestConfiguration;
  }

  public void setRunTestConfiguration(
      RunTestConfiguration runTestConfiguration) {
    this.runTestConfiguration = runTestConfiguration;
  }
}
