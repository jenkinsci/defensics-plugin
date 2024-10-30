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

package com.defensics.jenkins.configuration;

import static org.apache.commons.lang.StringUtils.isBlank;

import hudson.util.FormValidation;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StepConfigurationValidator {

  /**
   * Validation method for settings file path in job configuration.
   *
   * @param settingFilePath The settings file path user has entered in the form control.
   * @return Ok if the file path is set and has a valid extension, otherwise Error.
   */
  public FormValidation validateSettingFilePath(final String settingFilePath) {
    if (settingFilePath.isEmpty()) {
      return FormValidation.error("File path is not defined");
    } else if (!hasSettingFileExtension(settingFilePath)) {
      return FormValidation.error("File extension must be .set or .testplan.");
    } else {
      return FormValidation.ok();
    }
  }

  private boolean hasSettingFileExtension(String settingFilePath) {
    return settingFilePath.endsWith(".set") || settingFilePath.endsWith(".testplan");
  }

  /**
   * Validation method for configuration overrides in job configuration.
   *
   * @param configurationOverrides The overrides the user has entered
   * @return OK if the suite settings are somewhat in the format expected by Defensics, or empty.
   *         Otherwise Error.
   */
  public FormValidation validateConfigurationOverrides(String configurationOverrides) {
    if (isBlank(configurationOverrides)) {
      return FormValidation.ok();
    } else {

      String regex = "(\\s?--([^\\s]+)(\\s?)((('.*')|(\".*\"))|([^\\s]+))?)+";

      // Create a Pattern object
      Pattern r = Pattern.compile(regex);

      // Now create matcher object.
      Matcher m = r.matcher(configurationOverrides);

      if (!m.matches()) {
        return FormValidation.error(
            "Incorrect format. Check Defensics CLI documentation for correct format.");
      } else {
        return FormValidation.ok();
      }
    }
  }
}
