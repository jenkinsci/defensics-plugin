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
import io.crnk.core.resource.annotations.LookupIncludeBehavior;
import io.crnk.core.resource.annotations.RelationshipRepositoryBehavior;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Information about configuration used on some past test run
 * As this describes data about past event, (nearly) everything here should be read-only.
 */
@JsonApiResource(type = "result-test-configurations", nested = true)
public class ResultTestConfiguration extends BaseTestConfiguration {

  @JsonApiId
  @JsonApiRelationId
  private String resultId;

  @JsonApiRelation(
      lookUp = LookupIncludeBehavior.AUTOMATICALLY_WHEN_NULL,
      repositoryBehavior = RelationshipRepositoryBehavior.FORWARD_OWNER
  )
  @Schema(description = "Result this configuration was used in")
  @JsonApiField(postable = false, patchable = false)
  private Result result;

  @JsonApiRelation
  @Schema(description = "Main suite that was used")
  @JsonApiField(postable = false, patchable = false)
  private Suite suite;

  @JsonApiRelationId
  private String suiteId;

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

  public Result getResult() {
    return result;
  }

  public void setResult(Result result) {
    this.result = result;
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
}
