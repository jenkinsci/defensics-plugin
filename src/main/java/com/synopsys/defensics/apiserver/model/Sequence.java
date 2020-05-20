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
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@JsonApiResource(type = "sequences")
public class Sequence {

  @JsonApiId
  private String id;

  @Schema(description = "File containing the sequence", example = "user/request-response.seq")
  @JsonApiField(postable = false, patchable = false)
  private String file;

  @Schema(description = "Human-readable name of the sequence",
      example = "HTTP request and response")
  @JsonApiField(postable = false, patchable = false)
  private String name;

  @Schema(
      description = "More detailed description of sequence (note that this might be long and "
        + "include newlines)",
      example = "Send HTTP request and wait for HTTP response."
  )
  @JsonApiField(postable = false, patchable = false)
  private String description;

  @Schema(description = "Suite this file belongs to")
  @JsonApiRelation
  @JsonApiField(postable = false, patchable = false)
  private Suite suite;

  @JsonApiRelationId
  private String suiteId;

  @Schema(description = "Run configurations using this sequence")
  @JsonApiRelation(mappedBy = "sequence")
  @JsonApiField(postable = true, patchable = true)
  private List<RunTestConfiguration> runTestConfigurations;

  public Sequence() {
  }

  /**
   * Constructor.
   * @param id Sequence id
   * @param file Sequence file
   * @param name Sequence name
   * @param description Sequence description
   * @param suiteId Suite id
   */
  public Sequence(String id, String file, String name, String description, String suiteId) {
    this.id = id;
    this.file = file;
    this.name = name;
    this.description = description;
    this.suiteId = suiteId;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getFile() {
    return file;
  }

  public void setFile(String file) {
    this.file = file;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
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

  public List<RunTestConfiguration> getRunTestConfigurations() {
    return runTestConfigurations;
  }

  public void setRunTestConfigurations(
      List<RunTestConfiguration> runTestConfigurations) {
    this.runTestConfigurations = runTestConfigurations;
  }
}
