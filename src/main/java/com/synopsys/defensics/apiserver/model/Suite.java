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
import java.util.List;

/**
 * Defines the suite properties: specifier and version, and if it's installed.
 *
 * <p>When the suite starts, that started suite will be a suite instance {@link SuiteInstance}.</p>
 *
 * <p>Information on this is mostly read-only for clients; suiteInstance relation can be altered
 * to launch new instances of a suite.</p>
 */
public class Suite {

  @Schema(description = "Identifies the suite/version combination",
      example = "d3-http-server:4.13.0-rel-2020-03-22478")
  private String id;

  /**
   * Specifier of the suite.
   */
  @Schema(description = "Suite specifier (basically the id of a specific type of suite)",
      example = "d3-http-server")
  private String specifier;

  /**
   * Version of the suite.
   */
  @Schema(description = "Suite version", example = "4.13.0-rel-2020-03-22478")
  private String version;

  /**
   * Name of the suite.
   */
  @Schema(description = "Human-readable name of the suite", example = "HTTP Server Test Suite")
  private String name;

  /**
   * Constructor for suite.
   */
  public Suite(){

  }

  /**
   * Constructor for suite with initial values specified.
   * @param id Suite id
   * @param specifier Suite specifier.
   * @param version Suite version
   * @param name Suite name
   */
  public Suite(String id, String specifier, String version, String name) {
    this.id = id;
    this.specifier = specifier;
    this.version = version;
    this.name = name;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getSpecifier() {
    return specifier;
  }

  public void setSpecifier(String specifier) {
    this.specifier = specifier;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
