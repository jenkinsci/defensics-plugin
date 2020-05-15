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

import static org.apache.commons.lang.StringUtils.isNotBlank;

import com.synopsys.defensics.api.ApiService;
import com.synopsys.defensics.apiserver.model.Run;
import com.synopsys.defensics.apiserver.model.RunState;
import com.synopsys.defensics.apiserver.model.RunVerdict;
import com.synopsys.defensics.apiserver.model.SuiteInstance;
import com.synopsys.defensics.apiserver.model.SuiteRunState;
import com.synopsys.defensics.client.DefensicsRequestException;
import com.synopsys.defensics.jenkins.configuration.AuthenticationTokenProvider;
import com.synopsys.defensics.jenkins.configuration.InstanceConfiguration;
import com.synopsys.defensics.jenkins.result.HtmlReport;
import com.synopsys.defensics.jenkins.result.ResultPublisher;
import com.synopsys.defensics.jenkins.util.DefensicsUtils;
import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Plugin;
import hudson.model.Result;
import hudson.util.VersionNumber;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URL;
import java.nio.channels.ClosedByInterruptException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import jenkins.model.Jenkins;

/**
 * This class handles the process of starting a fuzz test with Defensics and monitoring its
 * progress.
 */
public class FuzzJobRunner {

  private ApiService defensicsClient;
  private PollingIntervals pollingIntervals;

  private Logger logger;

  /**
   * Run fuzz tests with Defensics and publish resulting HTML report.
   *
   * @param jenkinsRun             Jenkins run that started this.
   * @param workspace              Workspace of Jenkins job.
   * @param launcher               Launcher of Jenkins run.
   * @param testPlan               Fuzz testing is done according to this plan.
   * @param configurationOverrides String containing configuration overrides
   * @param instanceConfiguration  Configuration for Defensics instance that should run the tests.
   * @param saveResultPackage      Download and save result package for Defensics run?
   */
  public void run(hudson.model.Run<?, ?> jenkinsRun, FilePath workspace, Launcher launcher,
      Logger logger, FilePath testPlan, String configurationOverrides,
      InstanceConfiguration instanceConfiguration, boolean saveResultPackage)
      throws AbortException {
    this.logger = logger;

    // Denotes if job has been interrupted. If so, the interrupt flag should be reset after cleanup
    // has been done.
    boolean wasInterrupted = false;

    Run defensicsRun = null;
    Result runResult = null;

    try {
      pollingIntervals = new PollingIntervals(jenkinsRun, launcher.getListener(), logger);
      setUpDefensicsConnection(instanceConfiguration);

      logger.println("Creating new run.");
      defensicsRun = defensicsClient.createNewRun();

      logger.println("Uploading test configuration from " + testPlan);
      defensicsClient.uploadTestPlan(defensicsRun.getId(), testPlan);
      logger.println("Waiting for suite to load.");
      waitForSuiteLoading(defensicsRun);

      if (isNotBlank(configurationOverrides)) {
        logger.println(
            "Overriding test configuration file settings with values: " + configurationOverrides);
        defensicsClient.setTestConfigurationSettings(defensicsRun.getId(), configurationOverrides);
      }
      logger.println("Fuzz testing is starting.");
      defensicsClient.startRun(defensicsRun.getId());
      logger.println("Fuzz testing is RUNNING.");

      defensicsRun = trackRunStatus(defensicsRun.getId(), logger);
      logger.println("Fuzz testing is COMPLETED.");
      logger.println("Failures: " + DefensicsUtils.countRunFailures(defensicsRun));
      logger.println("Verdict: " + defensicsRun.getVerdict());

      publishResults(
          jenkinsRun,
          defensicsRun,
          workspace,
          testPlan.getName(),
          saveResultPackage);

      if (defensicsRun.getVerdict().equals(RunVerdict.PASS)) {
        runResult = Result.SUCCESS;
      } else {
        runResult = Result.FAILURE;
      }
      defensicsClient.deleteRun(defensicsRun.getId());
      defensicsRun = null;
    } catch (InterruptedException | ClosedByInterruptException | InterruptedIOException e) {
      // Let's clear the thread interrupted flag now, otherwise e.g. OkHttpClient doesn't do
      // any of the cleanup requests. Reset interrupt flag after cleanup.
      wasInterrupted = Thread.interrupted();

      handleRunInterruption(defensicsRun);
      runResult = Result.ABORTED;
    } catch (Exception e) {
      // The reason this throws an exception instead of logging error and setting build result
      // to failure, is so that users can do exception handling in pipeline scripts when there
      // are errors in the fuzzing process.
      throw new AbortException(e.getMessage() != null ? e.getMessage() : "");
    } finally {
      if (defensicsRun != null) {
        try {
          // Delete run if normal code path did not yet delete it.
          // If run is not deleted, the loaded suite and run will remain in the server
          defensicsClient.deleteRun(defensicsRun.getId());
        } catch (DefensicsRequestException e) {
          logger.logError("Could not delete run in API server");
        }
      }
      jenkinsRun.setResult(runResult != null ? runResult : Result.FAILURE);

      if (wasInterrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }

  /**
   * Does Defensics instance configuration and sets up the ApiService.
   */
  private void setUpDefensicsConnection(InstanceConfiguration instanceConfiguration)
      throws IOException, DefensicsRequestException {
    String authenticationToken = AuthenticationTokenProvider.getAuthenticationToken(
        new URL(instanceConfiguration.getUrl()), instanceConfiguration.getCredentialsId());

    defensicsClient = new ApiService(instanceConfiguration.getUrl(), authenticationToken,
        instanceConfiguration.isCertificateValidationDisabled());
    logger.println("Connecting to Defensics: " + instanceConfiguration.getName()
        + " (" + instanceConfiguration.getUrl() + ")");
    if (instanceConfiguration.isCertificateValidationDisabled()) {
      logger.println("Certificate validation is disabled.");
    }
    defensicsClient.healthCheck();
  }

  /**
   * Tracks running testrun and reports progress to Jenkins console log.
   *
   * @param runId  id of the test run to track
   * @param logger Logger to log to
   * @return true if test run was successful
   * @throws InterruptedException      If the process is interrupted while sleeping
   * @throws AbortException            If there is an issue communicating with server
   * @throws DefensicsRequestException If there is an issue communicating with server
   */
  private Run trackRunStatus(String runId, Logger logger)
      throws InterruptedException, IOException, DefensicsRequestException {
    long nextSleepDuration = pollingIntervals.getInitialRunPollingInterval();
    int errorCounter = 0;

    final RunLogger runLogger = new RunLogger(logger);
    while (errorCounter <= 10) {
      TimeUnit.SECONDS.sleep(nextSleepDuration);
      final Run run = defensicsClient.getRun(runId);

      switch (run.getState()) {
        case ERROR:
          throw new AbortException("Fuzzing failed.");
        case STARTING:
        case RUNNING:
          runLogger.log(run);
          nextSleepDuration = pollingIntervals.getRunPollingInterval();
          errorCounter = 0;
          break;
        case STOPPING:
        case COMPLETED:
          return run;
        default:
          nextSleepDuration = pollingIntervals.getRunPollingInterval();
          errorCounter++;
          break;
      }
    }

    // We got too many wrong run states, stop run.
    throw new AbortException(
        "Couldn't receive test run status from the Defensics, stopping the run.");
  }

  /**
   * Wait that Defensics suite for given run is loaded.
   *
   * @param run for which suite loading is waited
   */
  private void waitForSuiteLoading(Run run)
      throws IOException, InterruptedException, DefensicsRequestException {
    Optional<SuiteInstance> suiteInstanceMaybe =
        defensicsClient.getConfigurationSuite(run.getId());

    while (suiteInstanceMaybe.isPresent()
        && suiteInstanceMaybe.get().getState() == SuiteRunState.LOADING) {

      final SuiteInstance suiteInstance = suiteInstanceMaybe.get();

      if (suiteInstance.getState() == SuiteRunState.LOADING) {
        logger.println("Loading suite...");
      } else if (suiteInstance.getState() == SuiteRunState.ERROR) {
        throw new AbortException("Couldn't load the suite.");
      }

      TimeUnit.SECONDS.sleep(pollingIntervals.getTestplanLoadingInterval());
      suiteInstanceMaybe = defensicsClient.getConfigurationSuite(run.getId());
    }
    logger.println("Suite loaded.");
  }

  /**
   * Publish results. Handles publishing HTML report and adding actions to both build and job
   * level.
   *
   * @param jenkinsRun        The Jenkins run whose results are being published
   * @param defensicsRun      The Defensics run whose results are being published
   * @param workspace         Jenkins workspace, used to temporarily store report files.
   * @param testPlanName      Testplan filename. This is used as title for the report tab, which
   *                          helps identify results if there are multiple Defensics steps in the
   *                          Jenkins job.
   * @param saveResultPackage Save Defensics run results and provide link in build page?
   * @throws DefensicsRequestException If server responds with error
   * @throws IOException               If deleting the temporary report files in workspace fails
   * @throws InterruptedException      If deleting the temporary report files in workspace is
   *                                   interrupted.
   */
  public void publishResults(hudson.model.Run<?, ?> jenkinsRun, Run defensicsRun,
      FilePath workspace, String testPlanName, boolean saveResultPackage)
      throws Exception {
    // HTML report publishing doesn't work properly without HTML Publisher version 1.20 or newer.
    // With 1.19, for example, one result is published ok, but two results don't show up properly.
    // Plugin manager class name needs to be checked because of a bug in Jenkins test harness
    // (https://issues.jenkins-ci.org/browse/JENKINS-48885) that causes pluginmanager not to have
    // any plugins when running Jenkins test harness, even though the plugins are there and usable.
    Plugin htmlPublisherPlugin = Jenkins.get().getPlugin("htmlpublisher");
    if (!Jenkins.get().getPluginManager().getClass().getName()
        .equals("org.jvnet.hudson.test.TestPluginManager")
        && (htmlPublisherPlugin == null
        || htmlPublisherPlugin.getWrapper().getVersionNumber().compareTo(
        new VersionNumber("1.20")) < 0)) {
      logger.logError("Results can't be published without HTML Publisher Plugin 1.20 or newer. "
          + "Please install/update in Manage Jenkins > Manage Plugins. ");
      return;
    }

    logger.println("Downloading report.");
    final FilePath resultsDir = workspace.createTempDir("defensics-results", null);
    defensicsClient.saveResults(defensicsRun.getId(), resultsDir);
    final String resultFile = String
        .format("defensics-b%s-%s.zip", jenkinsRun.getId(), defensicsRun.getId());
    String resultPackageUrl = null;
    if (saveResultPackage) {
      logger.println("Downloading result package.");
      defensicsClient.saveResultPackage(resultsDir, resultFile, defensicsRun.getId());
    }

    HtmlReport report = null;
    try {
      report = new HtmlReport(resultsDir, defensicsRun.getId(), testPlanName);
      // ResultPublisher will move result files from workspace to job's build folder.
      // This includes result package if user has chosen to save it.
      new ResultPublisher().publishResults(
          jenkinsRun, defensicsRun, report, resultFile, logger, workspace);
    } finally {
      if (report != null) {
        if (saveResultPackage) {
          resultsDir.child(resultFile).delete();
        }
        report.delete();
      }
    }
  }

  /**
   * Handles that Defensics run is properly stopped if Jenkins run gets interrupted.
   */
  private void handleRunInterruption(Run run) {
    logger.println("Fuzzing was interrupted.");

    try {
      run = defensicsClient.getRun(run.getId());

      // We can't stop test run if suite isn't loaded so let's make sure it is
      final Optional<SuiteInstance> suiteMaybe = defensicsClient.getConfigurationSuite(run.getId());

      if (suiteMaybe.isPresent() && suiteMaybe.get().getState().equals(SuiteRunState.LOADING)) {
        logger.println("Suite loading is ongoing. Waiting for suite to load before unloading it.");
        waitForSuiteLoading(run);
      }

      // Only try to stop run if it's created and hasn't finished yet. Idle jobs cannot be stopped.
      if (run != null && run.getState() != RunState.COMPLETED && run.getState() != RunState.ERROR
        && run.getState() != RunState.IDLE) {
        final String runId = run.getId();
        logger.println("Stopping run.");
        defensicsClient.stopRun(runId);
        int errorCounter = 0;
        while (errorCounter <= 3) {
          TimeUnit.SECONDS.sleep(1);
          run = defensicsClient.getRun(runId);
          switch (run.getState()) {
            case ERROR:
              logger.logError("Test run is in error state, couldn't stop run.");
              return;
            case STARTING:
            case RUNNING:
            case STOPPING:
              errorCounter = 0;
              break;
            case COMPLETED:
              logger.println("Stopping succeeded.");
              return;
            default:
              errorCounter++;
              break;
          }
        }
      }
    } catch (DefensicsRequestException | IOException | InterruptedException exception) {
      logger.logError(
          "Couldn't track that run was COMPLETED, there is a possibility that run configuration "
              + "can't be removed automatically and suite will be left loaded!");
    }
  }
}