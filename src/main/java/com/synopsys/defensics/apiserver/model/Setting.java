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

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import java.util.ArrayList;
import java.util.List;

/**
 * Class to store setting object.
 */
public class Setting {
  /**
   * Name of the setting, used as id.
   */
  private String name;

  /**
   * Argument of the the setting.
   */
  private String argument;

  /**
   * Description for the setting. Used as human readable name.
   */
  private String description;

  /**
   * The settings group.
   */
  private String group;

  /**
   * List of dependencies to other settings.
   */
  private List<Dependency> dependency;

  /**
   * Possible list of choices.
   */
  private List<SettingChoice> choices;

  /**
   * Value of the suite.
   */
  private String value;

  /**
   * The default value.
   */
  private String defaultValue;

  /**
   * If the setting is enabled.
   */
  private boolean enabled;

  /**
   * If the setting is editable.
   */
  private boolean editable;

  /**
   * If change causes test cases change.
   */
  private boolean reloadRequired;

  /**
   * Type of the setting.
   */
  private String type;

  /**
   * Optional endpoint for additional data.
   */
  private String endpoint;

  /**
   * REGEX to be used for value validating.
   */
  private String validator;

  /**
   * URL for help.
   */
  private String helpUrl;

  /**
   * History of submitted values.
   */
  private List<String> history;

  /**
   * Possible setting children.
   * These are used for complex settings.
   */
  private List<Setting> children;

  /**
   * If the setting requires a value.
   */
  private boolean valueRequired;

  /**
   * Indicates support for live setting validation.
   */
  private boolean validationSupported;

  /**
   * Order of the setting.
   */
  private int order;

  public Setting() {}

  public void setArgument(String argument) {
    this.argument = argument;
  }

  @JsonSetter(nulls = Nulls.AS_EMPTY)
  public void setChoices(List<SettingChoice> choices) {
    this.choices = new ArrayList<>(choices);
  }

  public void setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
  }

  @JsonSetter(nulls = Nulls.AS_EMPTY)
  public void setDependency(List<Dependency> dependency) {
    this.dependency = dependency;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setEditable(boolean editable) {
    this.editable = editable;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

  public void setGroup(String group) {
    this.group = group;
  }

  public void setHelpUrl(String helpUrl) {
    this.helpUrl = helpUrl;
  }

  @JsonSetter(nulls = Nulls.AS_EMPTY)
  public void setHistory(List<String> history) {
    this.history = new ArrayList<>(history);
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setReloadRequired(boolean reloadRequired) {
    this.reloadRequired = reloadRequired;
  }

  public void setType(String type) {
    this.type = type;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public void setOrder(int order) {
    this.order = order;
  }

  public void setValidator(String validator) {
    this.validator = validator;
  }

  @JsonSetter(nulls = Nulls.AS_EMPTY)
  public void setChildren(List<Setting> children) {
    this.children = children;
  }

  public void setValueRequired(boolean valueRequired) {
    this.valueRequired = valueRequired;
  }

  public void setValidationSupported(boolean validationSupported) {
    this.validationSupported = validationSupported;
  }

  public String getDescription() {
    return description;
  }

  public String getName() {
    return name;
  }

  public String getArgument() {
    return argument;
  }

  public boolean getEditable() {
    return editable;
  }

  public boolean getEnabled() {
    return enabled;
  }

  public List<Dependency> getDependency() {
    return dependency;
  }

  public List<SettingChoice> getChoices() {
    return choices;
  }

  public String getDefaultValue() {
    return defaultValue;
  }

  public boolean getReloadRequired() {
    return reloadRequired;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public String getGroup() {
    return group;
  }

  public List<String> getHistory() {
    return history;
  }

  public String getHelpUrl() {
    return helpUrl;
  }

  public String getType() {
    return type;
  }

  public String getValidator() {
    return validator;
  }

  public String getValue() {
    return value;
  }

  public int getOrder() {
    return order;
  }

  public List<Setting> getChildren() {
    return children;
  }

  public boolean getValueRequired() {
    return valueRequired;
  }

  public boolean isValidationSupported() {
    return validationSupported;
  }
}