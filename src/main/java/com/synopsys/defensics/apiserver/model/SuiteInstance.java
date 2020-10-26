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

/**
 * Suite instance, e.g. loaded or running suite with id. The available and installed suites are
 * defined with {@link Suite}.
 */
public class SuiteInstance {
  private String id;

  private SuiteRunState state;
  private String error;

  // Must be specified on creation, can't be updated afterwards
  private Suite suite;

  private String suiteId;

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
}
