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
 * Information about configuration used on some past test run
 * As this describes data about past event, (nearly) everything here should be read-only.
 */
public class ResultTestConfiguration extends BaseTestConfiguration {

  @Schema(description = "Id of the result this configuration was used in")
  private String resultId;

  @Schema(description = "Id of the used main suite")
  private String suiteId;

  /**
   * Suite could use multiple sequences (e.g. SIP has different "Registration sequence",
   * "Used Notify sequence", etc. sequences). Other sequences are usually in support role, or more
   * akin to settings. Use here the main sequence which will be shown in the UI as the primary
   * sequence.
   */
  @Schema(description = "Sequence this configuration uses")
  private String sequenceId;

  public ResultTestConfiguration() {
  }

  /**
   * Constructor.
   *
   * @param name Configuration name
   * @param projectId Project ID
   * @param resultId Result ID
   * @param suiteId Suite ID
   * @param sequenceId Sequence ID
   */
  public ResultTestConfiguration(String name, String projectId, String resultId,
      String suiteId, String sequenceId) {
    super(name, projectId);
    this.resultId = resultId;
    this.suiteId = suiteId;
    this.sequenceId = sequenceId;
  }

  public String getResultId() {
    return resultId;
  }

  public void setResultId(String resultId) {
    this.resultId = resultId;
  }

  public String getSuiteId() {
    return suiteId;
  }

  public void setSuiteId(String suiteId) {
    this.suiteId = suiteId;
  }

  public String getSequenceId() {
    return sequenceId;
  }

  public void setSequenceId(String sequenceId) {
    this.sequenceId = sequenceId;
  }
}
