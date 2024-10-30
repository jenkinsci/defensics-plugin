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

import hudson.model.Descriptor.FormException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Validator for Defensics global configuration.
 */
public class InstanceConfigurationValidator {

  /**
   * Checks that Defensics global configuration is valid.
   *
   * @param defensicsInstances List of Defensics instance configurations to validate
   * @throws FormException If invalid data is found in the list.
   */
  public void validate(List<InstanceConfiguration> defensicsInstances) throws FormException {
    areNamesSet(defensicsInstances);
    areUrlsSet(defensicsInstances);
    areUrlsValid(defensicsInstances);
    areNamesUnique(defensicsInstances);
  }

  private void areNamesSet(List<InstanceConfiguration> defensicsInstances)
      throws FormException {
    long missingNameCount = defensicsInstances.stream().filter(
        instanceConfiguration ->
            instanceConfiguration.getName() == null
                || instanceConfiguration.getName().isEmpty()).count();
    if (missingNameCount > 0) {
      throw new FormException("Defensics instance name is empty", "name");
    }
  }

  private void areUrlsSet(List<InstanceConfiguration> defensicsInstances)
      throws FormException {
    long missingUrlsCount = defensicsInstances.stream().filter(
        instanceConfiguration ->
            instanceConfiguration.getUrl() == null
                || instanceConfiguration.getUrl().isEmpty()).count();
    if (missingUrlsCount > 0) {
      throw new FormException("Defensics instance URL is empty", "url");
    }
  }

  private void areUrlsValid(List<InstanceConfiguration> defensicsInstances)
      throws FormException {
    List<InstanceConfiguration> configurationsWithInvalidUrls =
        defensicsInstances.stream().filter(instanceConfiguration -> {
          try {
            new URL(instanceConfiguration.getUrl());
            return false;
          } catch (MalformedURLException e) {
            return true;
          }
        }).collect(Collectors.toList());
    if (configurationsWithInvalidUrls.size() > 0) {
      throw new FormException(
          "Defensics instance URL is not valid: "
              + configurationsWithInvalidUrls.stream().map(
                  InstanceConfiguration::getUrl)
              .collect(Collectors.joining(", ")),
          "url");
    }
  }

  private void areNamesUnique(List<InstanceConfiguration> defensicsInstances)
      throws FormException {
    List<String> duplicatedNames = new ArrayList<>();

    defensicsInstances.stream().collect(
        Collectors.groupingBy(InstanceConfiguration::getName)).forEach((name, instances) -> {
          if (instances.size() > 1) {
            duplicatedNames.add(name);
          }
        });

    if (duplicatedNames.size() > 0) {
      throw new FormException(
          "The Defensics instance name is already configured: "
              + duplicatedNames.stream().collect(Collectors.joining(", ")),
          "name");
    }
  }
}
