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

package com.synopsys.defensics.jenkins.test.utils;

import com.synopsys.defensics.jenkins.FuzzBuildStep;
import com.synopsys.defensics.jenkins.FuzzJobRunner;
import com.synopsys.defensics.jenkins.FuzzPostBuildStep;
import com.synopsys.defensics.jenkins.configuration.InstanceConfiguration;
import com.synopsys.defensics.jenkins.configuration.PluginConfiguration;
import hudson.FilePath;
import hudson.model.FreeStyleProject;
import hudson.model.TopLevelItem;
import java.io.InputStream;
import org.jvnet.hudson.test.JenkinsRule;

public class ProjectUtils {
  public static void setupProject(JenkinsRule jenkinsRule, TopLevelItem project,
      String defensicsName, String defensicsUrl, boolean certiciateValidationDisabled,
      String credentialsId,
      String settingFileName) throws Exception {
    addInstanceConfiguration(jenkinsRule, defensicsName, defensicsUrl, certiciateValidationDisabled,
        credentialsId);

    ProjectUtils.copyFileToWorkspace(jenkinsRule, project, settingFileName);
  }

  /**
   * Add another Defensics configuration.
   *
   * @param jenkinsRule                   The jenkinsrule used in the test. Used for getting
   *                                      DefensicsPublishStepDescriptor.
   * @param defensicsName                 Name of the Defensics configuration to add
   * @param defensicsUrl                  Url of the Defensics configuration to add
   * @param certificateValidationDisabled Is certificate validation disabled
   */
  public static void addInstanceConfiguration(
      JenkinsRule jenkinsRule, String defensicsName, String defensicsUrl,
      boolean certificateValidationDisabled, String credentialsId) {
    PluginConfiguration pluginConfiguration =
        jenkinsRule.get(PluginConfiguration.class);
    InstanceConfiguration instanceConfiguration = new InstanceConfiguration(defensicsName,
        defensicsUrl, certificateValidationDisabled, credentialsId);
    pluginConfiguration.getDefensicsInstances().add(instanceConfiguration);
  }

  /**
   * Add a build step to a project.
   *
   * @param project           Project to add build step to.
   * @param configurationName Name of Defensics to use.
   * @param settingFilePath   SettingFilePath relative to workspace.
   */
  public static void addBuildStep(FreeStyleProject project, String configurationName,
      String settingFilePath) {
    FuzzBuildStep plugin =
        new FuzzBuildStep(settingFilePath);
    plugin.setDefensicsInstance(configurationName);
    project.getBuildersList().add(plugin);
  }

  public static void addPostBuildStep(FreeStyleProject project, String configurationName,
      String settingFilePath) {
    FuzzPostBuildStep plugin =
        new FuzzPostBuildStep(settingFilePath);
    plugin.setDefensicsInstance(configurationName);
    project.getPublishersList().add(plugin);
  }

  /**
   * Copy a file to project workspace.
   *
   * @param jenkinsRule     Jenkinsrule used in the test. Used for getting workspace.
   * @param project         Project whose workspace we want to copy the file to.
   * @param settingFileName Filename of the file to copy. A file with this name must be found in
   *                        test resources, and this is also the name with which it will be saved
   *                        into workspace.
   * @throws Exception If there is a problem copying.
   */
  public static void copyFileToWorkspace(JenkinsRule jenkinsRule, TopLevelItem project,
      String settingFileName) throws Exception {
    FilePath workspace = jenkinsRule.jenkins.getWorkspaceFor(project);
    FilePath report = workspace.child(settingFileName);
    try (InputStream resourceAsStream = ProjectUtils.class.getResourceAsStream(settingFileName)) {
      report.copyFrom(resourceAsStream);
    }
  }
}
