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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Class to store setting object.
 */
public class Setting extends BaseSetting {
  /**
   * Argument of the the setting.
   */
  @Schema(description = "Command-line argument for the setting.")
  private String argument;

  /**
   * Description for the setting. Used as human readable name.
   */
  @Schema(description = "Human readable name of the setting.")
  private String description;

  /**
   * The settings group as list of groups, used internally.
   */
  @JsonIgnore
  private transient List<String> groupList;

  /**
   * The settings group id.
   */
  @Schema(description = "Name of the settings group (page) this setting is listed in.")
  private String group;

  /**
   * Possible list of choices.
   */
  @Schema(description = "Allowed choice values for this setting.")
  private List<SettingChoice> choices;

  /**
   * The default value.
   */
  @Schema(description = "Default value for this setting.")
  private String defaultValue;

  /**
   * If the setting is enabled.
   */
  @Schema(description = "A flag indicating if the setting is configurable (enabled) or not.")
  private boolean enabled;

  /**
   * If change causes test cases change.
   */
  @Schema(description = "A flag indicating if changing the value of this setting will require the"
      + " suite to be reloaded.")
  private boolean reloadRequired;

  /**
   * Type of the setting.
   */
  @Schema(description = "Type of this setting.")
  private String type;

  /**
   * URL for help.
   */
  @Schema(description = "URL for help documentation for this setting.")
  private String helpUrl;

  /**
   * History of submitted values.
   */
  @JsonIgnore
  @Schema(description = "History of recently used values for this setting. History is never "
      + "available for PASSWORD, CHOICE, BOOLEAN or INDEX settings.")
  private List<String> history;

  /**
   * If the setting requires a value.
   */
  @Schema(description = "Indicate if a value is always required for this setting.")
  private boolean valueRequired;

  /**
   * Indicates support for live setting validation.
   */
  @Schema(description = "A flag indicating if live validation is supported for this setting.")
  private boolean validationSupported;

  /**
   * Number size, a rendering hint for frontend.
   */
  @Schema(description = "Size hint for a number setting field.")
  private NumberSize numberSize;

  /**
   * Minimum value for number setting. {@code null} if not defined.
   */
  @Schema(description = "Minimum value for a number setting.")
  private BigInteger minValue;

  /**
   * Maximum value for number setting. {@code null} if not defined.
   */
  @Schema(description = "Maximum value for a number setting.")
  private BigInteger maxValue;

  /**
   * Number radix. {@code null} if not defined.
   */
  @Schema(description = "Radix for a number setting.")
  private Integer radix;

  /**
   * Stepping buttons rendering hint.
   */
  @Schema(description = "Should increment/decrement buttons be shown for this setting.")
  private Boolean steppable;

  /**
   * Order of the setting.
   */
  @Schema(description = "Location of this setting in an UI. Smaller values should be shown first.")
  private int order;

  public Setting() {
    super(null, null);
  }

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

  public void setDescription(String description) {
    this.description = description;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * Setter for the group list, sets also the group id.
   *
   * @param groupList list of groups.
   */
  public void setGroupList(List<String> groupList) {
    this.groupList = groupList;
    group = joinGroupList(groupList);
  }

  /**
   * Joins the group list as a single string.
   * @param groupList the joined list.
   * @return a single string for the group.
   */
  public static String joinGroupList(List<String> groupList) {
    if (groupList != null && groupList.size() > 0) {
      return String.join("|", groupList);
    }
    return null;
  }

  public void setHelpUrl(String helpUrl) {
    this.helpUrl = helpUrl;
  }

  @JsonSetter(nulls = Nulls.AS_EMPTY)
  public void setHistory(List<String> history) {
    this.history = new ArrayList<>(history);
  }

  public void setReloadRequired(boolean reloadRequired) {
    this.reloadRequired = reloadRequired;
  }

  public void setType(String type) {
    this.type = type;
  }

  public void setOrder(int order) {
    this.order = order;
  }

  public void setValueRequired(boolean valueRequired) {
    this.valueRequired = valueRequired;
  }

  public void setValidationSupported(boolean validationSupported) {
    this.validationSupported = validationSupported;
  }

  public NumberSize getNumberSize() {
    return numberSize;
  }

  public BigInteger getMinValue() {
    return minValue;
  }

  public BigInteger getMaxValue() {
    return maxValue;
  }

  public Integer getRadix() {
    return radix;
  }

  public Boolean isSteppable() {
    return steppable;
  }

  public String getDescription() {
    return description;
  }

  public String getArgument() {
    return argument;
  }

  public boolean getEnabled() {
    return enabled;
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

  public List<String> getGroupList() {
    return groupList;
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

  public int getOrder() {
    return order;
  }

  public boolean getValueRequired() {
    return valueRequired;
  }

  public boolean isValidationSupported() {
    return validationSupported;
  }

  public void setNumberSize(NumberSize numberSize) {
    this.numberSize = numberSize;
  }

  public void setMinValue(BigInteger minValue) {
    this.minValue = minValue;
  }

  public void setMaxValue(BigInteger maxValue) {
    this.maxValue = maxValue;
  }

  public void setRadix(Integer radix) {
    this.radix = radix;
  }

  public void setSteppable(Boolean steppable) {
    this.steppable = steppable;
  }
}