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

/**
 * Class for setting choice.
 */
public class SettingChoice {

  /**
   * Constructor.
   *
   * @param name        name for choice.
   * @param description description for the choice.
   */
  public SettingChoice(String name, String description) {
    this.name = name;
    this.description = description;
  }

  /**
   * Empty constructor.
   */
  public SettingChoice() {

  }


  /**
   * Name of the choice.
   */
  String name;

  /**
   * Description of the choice.
   */
  String description;

  public void setName(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
