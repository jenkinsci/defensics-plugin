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

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.synopsys.defensics.jenkins.configuration.InstanceConfiguration;
import com.synopsys.defensics.jenkins.configuration.PluginConfiguration;
import com.synopsys.defensics.jenkins.configuration.StepConfigurationValidator;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.AbortException;
import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

/**
 * Pipeline step for defensics fuzz job.
 *
 * <p>This uses the asynchronous Step and StepExecution
 * (https://plugins.jenkins.io/workflow-step-api/) instead of previously used SimpleBuildStep
 * because it allows better handling of step lifecycle, especially in aborted jobs. SimpleBuildStep
 * had IllegalStateException issues where same step received both onSuccess and onFailure events,
 * now only onSuccess or onFailure event should be return.
 * </p>
 */
public class FuzzPipelineStep extends Step {
  /**
   * Step name used e.g. in pipeline scripts.
   */
  public static final String STEP_NAME = "defensics";

  /**
   * Fuzz run configuration overrides.
   */
  private String configurationOverrides = "";

  /**
   * Used Defensics server instance.
   */
  private String selectedDefensicsInstanceName;

  /**
   * Denotes if fuzz run result package should be downloaded.
   */
  private boolean saveResultPackage = false;

  /**
   * Defensics testplan used.
   */
  private final String configurationFilePath;

  /**
   * Constructor.
   *
   * @param configurationFilePath Path to .set (or .testplan) file to use for testing, relative to
   *                        workspace.
   */
  @DataBoundConstructor
  public FuzzPipelineStep(String configurationFilePath) {
    this.configurationFilePath = configurationFilePath;
  }

  public String getDefensicsInstance() {
    return selectedDefensicsInstanceName;
  }

  @DataBoundSetter
  public void setDefensicsInstance(@NonNull String defensicsInstanceName) {
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
  public StepExecution start(StepContext context) {
    final FuzzPipelineStepExecution fuzzPipelineStepExecution = new FuzzPipelineStepExecution(
        context,
        this
    );
    return fuzzPipelineStepExecution;
  }

  /**
   * Actual Execution class. Handles step lifecycle.
   */
  private static class FuzzPipelineStepExecution extends StepExecution {
    /** Serial Version UID as recommended by https://plugins.jenkins.io/workflow-step-api/ .*/
    private static final long serialVersionUID = 1L;

    /**
     * Pipeline object containing run parameters.
     */
    private final transient FuzzPipelineStep fuzzPipelineStep;

    /**
     * Fuzz job future to monitor and control fuzz job lifecycle.
     */
    private transient Future<?> future;

    protected FuzzPipelineStepExecution(
        @NonNull StepContext context,
        FuzzPipelineStep fuzzPipelineStep
    ) {
      super(context);
      this.fuzzPipelineStep = fuzzPipelineStep;
    }

    /**
     * Starts fuzz job asynchronously with supplied run parameters and stores the job future.
     *
     * @return false if job had not finished yet.
     * @throws Exception if issues in job startup
     */
    @Override
    public boolean start() throws Exception {
      final Run run = getContext().get(Run.class);
      final FilePath workspace = getContext().get(FilePath.class);
      final Launcher launcher = getContext().get(Launcher.class);
      final TaskListener listener = getContext().get(TaskListener.class);
      if (workspace == null) {
        throw new IllegalArgumentException("Workspace was null");
      }

      final ExecutorService executorService = Executors.newSingleThreadExecutor();
      future = executorService.submit(() -> {
        try {
          final FuzzStep fuzzStep = new FuzzStep(
              fuzzPipelineStep.getDescriptor(),
              fuzzPipelineStep.getDefensicsInstance(),
              fuzzPipelineStep.configurationFilePath,
              fuzzPipelineStep.configurationOverrides,
              fuzzPipelineStep.saveResultPackage
          );
          fuzzStep.perform(
              run,
              workspace,
              launcher,
              listener
          );
          // NOTE: The resultValue for onSuccess is not clearly defined, so returning null
          // for now since similar getContext().onSuccess(run()) has been used in other plugins
          // where run() returns null.
          getContext().onSuccess(null);
        } catch (AbortException e) {
          getContext().onFailure(e);
        } catch (Exception e) {
          getContext().onFailure(e);
        }
      });
      return false;
    }

    /**
     * Called when job should be stopped.
     *
     * @param cause for stop (e.g. User stop)
     */
    @Override
    public void stop(@NonNull Throwable cause) {
      if (future != null) {
        future.cancel(true);
      }
    }
  }

  @Override
  public FuzzPipelineDescriptor getDescriptor() {
    return (FuzzPipelineDescriptor)super.getDescriptor();
  }

  /**
   * Descriptor for pipeline step.
   */
  @Extension
  @Symbol(STEP_NAME)
  public static final class FuzzPipelineDescriptor
      extends StepDescriptor
      implements ExtensionPoint, FuzzStepDescriptor {

    @Inject
    private PluginConfiguration pluginConfiguration;

    @Inject
    private transient FuzzJobRunner fuzzJobRunner;

    @Inject
    private StepConfigurationValidator stepConfigurationValidator;

    public FuzzPipelineDescriptor() {
      super.load();
    }

    /**
     * Defines what contexts need to be present so that run can start.
     *
     * @return Requires contexts
     */
    @Override
    public Set<? extends Class<?>> getRequiredContext() {
      return ImmutableSet.of(
          FilePath.class,
          Launcher.class,
          Run.class,
          TaskListener.class
      );
    }

    @Override
    public String getFunctionName() {
      return STEP_NAME;
    }

    @NonNull
    @Override
    public String getDisplayName() {
      return PluginConfiguration.DISPLAY_NAME;
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
