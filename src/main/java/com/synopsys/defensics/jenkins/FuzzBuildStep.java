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

package com.synopsys.defensics.jenkins;

import com.google.inject.Inject;
import com.synopsys.defensics.jenkins.configuration.InstanceConfiguration;
import com.synopsys.defensics.jenkins.configuration.PluginConfiguration;
import com.synopsys.defensics.jenkins.configuration.StepConfigurationValidator;
import hudson.AbortException;
import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.util.List;
import edu.umd.cs.findbugs.annotations.NonNull;
import jenkins.tasks.SimpleBuildStep;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

/**
 * The main build step of this plugin.
 */
public class FuzzBuildStep extends Builder implements SimpleBuildStep {

  private final String configurationFilePath;
  private String selectedDefensicsInstanceName;
  private String configurationOverrides = "";
  private boolean saveResultPackage = false;

  /**
   * Constructor.
   *
   * @param configurationFilePath Path to .set (or .testplan) file to use for testing, relative to
   *                        workspace.
   */
  @DataBoundConstructor
  public FuzzBuildStep(String configurationFilePath) {
    this.configurationFilePath = configurationFilePath;
  }

  public String getDefensicsInstance() {
    return selectedDefensicsInstanceName;
  }

  @DataBoundSetter
  public void setDefensicsInstance(
      @NonNull String defensicsInstanceName) {
    this.selectedDefensicsInstanceName = defensicsInstanceName;
  }

  public String getConfigurationOverrides() {
    return configurationOverrides;
  }

  @DataBoundSetter
  public void setConfigurationOverrides(String configurationOverrides) {
    this.configurationOverrides = configurationOverrides;
  }

  public String getConfigurationFilePath() {
    return configurationFilePath;
  }

  public boolean isSaveResultPackage() {
    return saveResultPackage;
  }

  @DataBoundSetter
  public void setSaveResultPackage(boolean saveResultPackage) {
    this.saveResultPackage = saveResultPackage;
  }

  @Override
  public void perform(@NonNull Run<?, ?> run, @NonNull FilePath workspace,
      @NonNull Launcher launcher, @NonNull TaskListener listener)
      throws AbortException, InterruptedException {
    FuzzStep fuzzStep = new FuzzStep(
        getDescriptor(),
        selectedDefensicsInstanceName,
        configurationFilePath,
        configurationOverrides,
        saveResultPackage);
    try {
      fuzzStep.perform(run, workspace, launcher, listener);
    } catch (AbortException e) {
      // SimpleBuildStep.perform(...) JavaDocs says method should throw InterruptedException
      // if interrupted so translate exception in this case, otherwise throw original exception
      if (Result.ABORTED.equals(run.getResult())) {
        throw new InterruptedException(e.getMessage());
      } else {
        throw e;
      }
    }
  }

  @Override
  public FuzzBuildStepDescriptor getDescriptor() {
    return (FuzzBuildStepDescriptor) super.getDescriptor();
  }

  @Extension
  public static final class FuzzBuildStepDescriptor extends BuildStepDescriptor<Builder>
      implements ExtensionPoint, FuzzStepDescriptor {

    @Inject
    private PluginConfiguration pluginConfiguration;
    @Inject
    private StepConfigurationValidator stepConfigurationValidator;
    @Inject
    private transient FuzzJobRunner fuzzJobRunner;

    public FuzzBuildStepDescriptor() {
      super.load();
    }

    public List<InstanceConfiguration> getDefensicsInstances() {
      return pluginConfiguration.getDefensicsInstances();
    }

    public FuzzJobRunner getFuzzJobRunner() {
      return fuzzJobRunner;
    }

    public void setFuzzJobRunner(FuzzJobRunner fuzzJobRunner) {
      this.fuzzJobRunner = fuzzJobRunner;
    }

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> jobType) {
      return true;
    }

    @NonNull
    @Override
    public String getDisplayName() {
      return PluginConfiguration.DISPLAY_NAME;
    }

    /**
     * This method is called by Jenkins to get the options for selecting Defensics instance for a
     * job.
     *
     * @return The items to present in the select.
     */
    public ListBoxModel doFillDefensicsInstanceItems() {
      return pluginConfiguration.doFillDefensicsInstanceNameItems();
    }

    /**
     * Validation method for settings file path in job configuration. Called by Jenkins.
     *
     * @param configurationFilePath The settings file path user has entered in the form control.
     * @return Ok if the file path is set and has a valid extension, otherwise Error.
     */
    public FormValidation doCheckConfigurationFilePath(
        @QueryParameter final String configurationFilePath) {
      return stepConfigurationValidator.validateSettingFilePath(configurationFilePath);
    }

    public FormValidation doCheckConfigurationOverrides(
        @QueryParameter final String configurationOverrides) {
      return stepConfigurationValidator.validateConfigurationOverrides(configurationOverrides);
    }
  }
}
