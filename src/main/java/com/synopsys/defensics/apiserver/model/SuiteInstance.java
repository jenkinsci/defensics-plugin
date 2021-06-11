/*
 * Copyright © 2020-2021 Synopsys, Inc.
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
 * Suite instance, e.g. loaded or running suite with id. The available and installed suites are
 * defined with {@link Suite}.
 */
@Schema(
    description = "This is a single loaded instance of the suite. If the system has multiple copies "
        + "of the same suite loaded, each will have it's own suite instance, pointing to "
        + "the same suite (via feature/version reference)."
)
public class SuiteInstance {
  @Schema(
      description = "ID for this suite instance",
      example = "f7469afe-db02-49e1-99a3-901dc126599a"
  )
  private String id;

  @Schema(
      description = "Current state of the suite",
      example = "LOADING"
  )
  private RunState state;
  @Schema(
      description = "Error message describing reason why suite was transitioned to 'ERROR' state"
  )
  private String error;

  @Schema(
      description = "The feature of the suite this is instance of",
      example = "d3-http-server"
  )
  private String suiteFeature;
  @Schema(
      description = "The version of suite this is instance of",
      example = "4.13.0"
  )
  private String suiteVersion;

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
   * @param suiteFeature Feature of the suite this is instance of
   * @param suiteVersion Version of the suite this is instance of
   */
  public SuiteInstance(
      String id,
      RunState state,
      String error,
      String suiteFeature,
      String suiteVersion) {
    this.id = id;
    this.state = state;
    this.error = error;
    this.suiteFeature = suiteFeature;
    this.suiteVersion = suiteVersion;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public RunState getState() {
    return state;
  }

  public void setState(RunState state) {
    this.state = state;
  }

  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }

  public String getSuiteFeature() {
    return suiteFeature;
  }

  public String getSuiteVersion() {
    return suiteVersion;
  }
}
