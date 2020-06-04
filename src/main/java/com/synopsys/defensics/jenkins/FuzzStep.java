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

package com.synopsys.defensics.jenkins;

import com.synopsys.defensics.jenkins.configuration.InstanceConfiguration;
import com.synopsys.defensics.jenkins.configuration.MissingConfigurationException;
import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.io.IOException;
import java.util.List;

public class FuzzStep {

  private final FuzzStepDescriptor descriptor;
  private final String selectedDefensicsInstanceName;
  private final String settingFilePath;
  private final String configurationOverrides;
  private final boolean saveResultPackage;

  FuzzStep(FuzzStepDescriptor descriptor, String selectedDefensicsInstanceName,
      String settingFilePath, String configurationOverrides, boolean saveResultPackage) {
    this.descriptor = descriptor;
    this.selectedDefensicsInstanceName = selectedDefensicsInstanceName;
    this.settingFilePath = settingFilePath;
    this.configurationOverrides = configurationOverrides;
    this.saveResultPackage = saveResultPackage;
  }

  void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
      throws AbortException {
    Logger logger = new Logger(listener);
    FilePath testPlan;
    InstanceConfiguration configuration;
    try {
      testPlan = getTestPlan(workspace);
      configuration = getDefensicsInstance(selectedDefensicsInstanceName);
    } catch (MissingConfigurationException e) {
      logger.logError(e.getMessage());
      throw new AbortException(e.getMessage());
    }

    new FuzzJobRunner().run(
        run, workspace, launcher, logger, testPlan, configurationOverrides, configuration,
        saveResultPackage);
  }


  private FilePath getTestPlan(FilePath workspace) throws MissingConfigurationException {
    FilePath testPlan = new FilePath(workspace, settingFilePath);
    try {
      if (!testPlan.exists()) {
        throw new MissingConfigurationException("File does not exist: " + testPlan);
      }
    } catch (IOException | InterruptedException e) {
      throw new MissingConfigurationException(
          "Unable to check existence of " + testPlan + ": " + e.getMessage());
    }
    return testPlan;
  }

  /**
   * Get the Defensics instance based on its name.
   *
   * @param defensicsInstanceName The name of the Defensics as configured in Jenkins' global
   *                              configuration.
   * @return The configuration for the named Defensics instance.
   * @throws MissingConfigurationException If an instance can't be found with the given name.
   */
  InstanceConfiguration getDefensicsInstance(
      String defensicsInstanceName)
      throws MissingConfigurationException {
    List<InstanceConfiguration> defensicsInstances = descriptor.getDefensicsInstances();
    if (defensicsInstances.size() == 0) {
      throw new MissingConfigurationException("No Defensics instances configured.");
    }

    if (defensicsInstanceName == null) {
      return defensicsInstances.get(0);
    }

    return defensicsInstances.stream().filter(instanceConfiguration ->
        instanceConfiguration.getName().equals(defensicsInstanceName)
    ).findFirst().orElseThrow(() ->
        new MissingConfigurationException(
            "Defensics instance '" + defensicsInstanceName
                + "' doesn't exist."));
  }
}
