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

public class Sequence {

  private String id;

  @Schema(description = "File containing the sequence", example = "user/request-response.seq")
  private String file;

  @Schema(description = "Human-readable name of the sequence",
      example = "HTTP request and response")
  private String name;

  @Schema(
      description = "More detailed description of sequence (note that this might be long and "
        + "include newlines)",
      example = "Send HTTP request and wait for HTTP response."
  )
  private String description;

  @Schema(description = "Id of the suite this file belongs to")
  private String suiteId;

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

  public String getSuiteId() {
    return suiteId;
  }

  public void setSuiteId(String suiteId) {
    this.suiteId = suiteId;
  }
}
