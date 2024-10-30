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

package com.defensics.apiserver.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Defines the suite properties: feature and version, and if it's installed.
 *
 * <p>When the suite starts, that started suite will be a suite instance {@link SuiteInstance}.</p>
 *
 * <p>Information on this is mostly read-only for clients.</p>
 */
@Schema(
    description = "Installed suite that can be loaded. Suite is identified "
        + "by its feature/version combination"
)
public class Suite {

  /**
   * Feature of the suite.
   */
  @Schema(description = "Suite feature identifies type of the suite (HTTP suite, TLS suite, etc)",
      example = "d3-http-server")
  private String feature;

  /**
   * Version of the suite.
   */
  @Schema(description = "Suite version", example = "4.13.0")
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
   * @param feature Suite feature.
   * @param version Suite version
   * @param name Suite name
   */
  public Suite(String feature, String version, String name) {
    this.feature = feature;
    this.version = version;
    this.name = name;
  }

  public String getFeature() {
    return feature;
  }

  public void setFeature(String feature) {
    this.feature = feature;
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
