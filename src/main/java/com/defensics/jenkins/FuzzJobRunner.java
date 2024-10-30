/*
 * Copyright 2024 Black Duck Software, Inc. All rights reserved.
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

package com.defensics.jenkins;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import com.defensics.api.ApiService;
import com.defensics.apiserver.model.HealthCheckResult;
import com.defensics.apiserver.model.Run;
import com.defensics.apiserver.model.RunState;
import com.defensics.apiserver.model.RunVerdict;
import com.defensics.apiserver.model.SuiteInstance;
import com.defensics.client.DefensicsRequestException;
import com.defensics.jenkins.configuration.AuthenticationTokenProvider;
import com.defensics.jenkins.configuration.InstanceConfiguration;
import com.defensics.jenkins.result.HtmlReport;
import com.defensics.jenkins.result.ResultPackageAction;
import com.defensics.jenkins.result.ResultPublisher;
import com.defensics.jenkins.util.DefensicsUtils;
import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Plugin;
import hudson.PluginManager;
import hudson.model.Result;
import hudson.util.VersionNumber;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URL;
import java.nio.channels.ClosedByInterruptException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
   * Default constructor.
   *
   * <p>TODO: Check later if this can be augmented to take all related service objects as
   * constructor arguments so writing unit tests would be cleaner. Some related service objects
   * require jenkinsRun information etc., so need to check how this object lifecycle goes.
   * </p>
   */
  public FuzzJobRunner() {
  }

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

    boolean reportDownloaded = false;
    boolean resultPackageDownloaded = false;

    Run defensicsRun = null;
    Result runResult = null;

    try {
      pollingIntervals = getPollingIntervals(jenkinsRun, launcher, logger);
      setUpDefensicsConnection(instanceConfiguration);

      logger.println("Creating new run.");
      defensicsRun = defensicsClient.createNewRun();

      logger.println("Uploading test configuration from " + testPlan);
      defensicsClient.uploadTestPlan(defensicsRun.getId(), testPlan);

      final String suiteLoadingMessage = defensicsClient.getSuiteInformationForRun(defensicsRun)
          .map(suite -> String.format("Waiting for %s %s suite to load.",
              suite.getName(),
              suite.getVersion()
              )
          ).orElse("Waiting for suite to load.");

      logger.println(suiteLoadingMessage);
      waitForSuiteLoading(defensicsRun);

      if (isNotBlank(configurationOverrides)) {
        logger.println(
            "Overriding test configuration file settings with values: " + configurationOverrides);
        defensicsClient.setTestConfigurationSettings(defensicsRun.getId(), configurationOverrides);
        // Some settings require reload so check if suite is reloading and wait its completion
        SuiteInstance suiteInstance = defensicsClient.getConfigurationSuite(defensicsRun.getId())
            .orElseThrow(() -> new AbortException("Defensics suite not found anymore"));
        if (suiteInstance.getState().equals(RunState.LOADING)) {
          logger.println("Used setting requires suite reload");
          waitForSuiteLoading(defensicsRun);
        }
      }
      logger.println("Fuzz testing is starting.");
      defensicsClient.startRun(defensicsRun.getId());
      logger.println("Fuzz testing is RUNNING.");

      defensicsRun = trackRunStatus(defensicsRun.getId(), logger);

      if (defensicsRun.getState().equals(RunState.COMPLETED))  {
        logger.println("Fuzz testing is COMPLETED.");
      } else {
        logger.logError(String.format("Test run terminated with %s.", defensicsRun.getState()));
        logRunErrorMessage(logger, defensicsRun);
      }

      logger.println("Failures: " + DefensicsUtils.countRunFailures(defensicsRun));
      logger.println("Verdict: " + defensicsRun.getVerdict());

      publishResults(jenkinsRun, defensicsRun, workspace, testPlan.getName());
      reportDownloaded = true;

      if (saveResultPackage) {
        publishResultPackage(jenkinsRun, defensicsRun, testPlan);
        resultPackageDownloaded = true;
      }

      if (defensicsRun.getVerdict().equals(RunVerdict.PASS)
          && defensicsRun.getState().equals(RunState.COMPLETED)
      ) {
        runResult = Result.SUCCESS;
        defensicsClient.deleteRun(defensicsRun.getId());
        defensicsRun = null;
      } else {
        RunVerdict verdict = defensicsRun.getVerdict();
        int failureCount = DefensicsUtils.countRunFailures(defensicsRun);
        defensicsClient.deleteRun(defensicsRun.getId());
        defensicsRun = null;
        throw new AbortException("Fuzzing completed with verdict " + verdict
            + " and " + failureCount + " failures. "
            + "See Defensics Results for details.");
      }
    } catch (InterruptedException | ClosedByInterruptException | InterruptedIOException e) {
      // Let's clear the thread interrupted flag now, otherwise e.g. HttpClient doesn't do
      // any of the cleanup requests. Reset interrupt flag after cleanup.
      wasInterrupted = Thread.interrupted();

      if (defensicsRun != null) {
        handleRunInterruption(defensicsRun);

        if (!reportDownloaded || (saveResultPackage && !resultPackageDownloaded)) {
          try {
            // Refresh run to get latest state and try to retrieve results. As the build was
            // interrupted, this may not succeed if user/jenkins stops the build after interrupt.
            defensicsRun = defensicsClient.getRun(defensicsRun.getId());
            if (defensicsRun != null && defensicsRun.getResultId() != null) {
              logger.println("Downloading results for the interrupted job");
              if (!reportDownloaded) {
                publishResults(jenkinsRun, defensicsRun, workspace, testPlan.getName());
              }
              if (saveResultPackage && !resultPackageDownloaded) {
                publishResultPackage(jenkinsRun, defensicsRun, testPlan);
              }
            }
          } catch (Exception ex) {
            logger.logError("Could not save results for the interrupted job: " + ex.getMessage());
          }
        }
      }
      runResult = Result.ABORTED;
      throw new AbortException("Fuzzing was interrupted.");
    } catch (Exception e) {
      runResult = Result.FAILURE;
      if (e instanceof AbortException) {
        throw (AbortException) e;
      }
      logger.logError(e.getMessage());
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
          logger.println("Unloaded suite and deleted the run from API server");
        } catch (DefensicsRequestException | InterruptedException e) {
          logger.logError("Could not delete run in API server: " + e.getMessage());
        }
      }

      if (runResult == null) {
        throw new AbortException("Fuzzing failed for unknown reason.");
      } else if (runResult != Result.FAILURE) {
        jenkinsRun.setResult(runResult);
      }

      if (wasInterrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }

  /**
   * Inspects given run and its suite and logs an error message if present.
   *
   * @param logger Logger to print urgent information
   * @param defensicsRun Run to inspect
   * @throws InterruptedException if processing was interrupted
   */
  private void logRunErrorMessage(Logger logger, Run defensicsRun) throws InterruptedException {
    try {
      // Check if there's more error information in suite record
      defensicsClient.getConfigurationSuite(defensicsRun.getId())
          .ifPresent(suiteInstance -> {
            if (suiteInstance.getError() != null && !suiteInstance.getError().isEmpty()) {
              logger.logError("Suite error: " + suiteInstance.getError());
            }
          });
    } catch (DefensicsRequestException e) {
      logger.logError("Could not get suite error information: " + e.getMessage());
    }
  }

  /**
   * Does Defensics instance configuration and sets up the ApiService.
   */
  private void setUpDefensicsConnection(InstanceConfiguration instanceConfiguration)
      throws IOException, DefensicsRequestException, InterruptedException {
    String authenticationToken = AuthenticationTokenProvider.getAuthenticationToken(
        new URL(instanceConfiguration.getUrl()), instanceConfiguration.getCredentialsId());

    defensicsClient = getApiService(instanceConfiguration, authenticationToken);
    logger.println("Connecting to Defensics: " + instanceConfiguration.getName()
        + " (" + instanceConfiguration.getUrl() + ")");
    if (instanceConfiguration.isCertificateValidationDisabled()) {
      logger.println("Certificate validation is disabled.");
    }

    final Map<String, HealthCheckResult> failingHealthChecks =
        defensicsClient.getFailingHealthChecks();
    if (!failingHealthChecks.isEmpty()) {
      final String serverStatusWarningMessage =
          "Defensics server has following unhealthy health checks which may affect server operation:\n"
              + ApiService.formatUnhealthyHealthCheckLines(failingHealthChecks);
      logger.logWarning(serverStatusWarningMessage);
    }

    defensicsClient.getServerVersion().ifPresent(serverVersion -> {
      logger.println("Defensics server version: " + serverVersion);
    });
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
        case FATAL:
        case ERROR:
          runLogger.log(run);
          return run;
        case STARTING:
        case RUNNING:
          runLogger.log(run);
          nextSleepDuration = pollingIntervals.getRunPollingInterval();
          errorCounter = 0;
          break;
        case UNLOADING:
        case STOPPING:
        case COMPLETED:
          runLogger.log(run);
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

    SuiteInstance suiteInstance = defensicsClient.getConfigurationSuite(run.getId())
            .orElseThrow(() -> new AbortException("Suite not found"));

    while (suiteInstance.getState() == RunState.LOADING) {
      logger.println("Loading suite...");
      TimeUnit.SECONDS.sleep(pollingIntervals.getTestplanLoadingInterval());
      suiteInstance = defensicsClient.getConfigurationSuite(run.getId())
          .orElseThrow(() -> new AbortException("Suite not found"));
    }

    if (Arrays.asList(RunState.ERROR, RunState.FATAL).contains(suiteInstance.getState())) {
      String errorMessage = "Couldn't load suite";
      if (suiteInstance.getError() != null && !suiteInstance.getError().isEmpty()) {
        errorMessage += ", error: " + suiteInstance.getError();
      }
      throw new AbortException(errorMessage);
    }

    logger.println("Suite loaded.");
  }

  /**
   * Publish results. Handles publishing HTML report and adding actions to both build and job
   * level.
   *
   * @param jenkinsRun   The Jenkins run whose results are being published
   * @param defensicsRun The Defensics run whose results are being published
   * @param workspace    Jenkins workspace, used to temporarily store report files.
   * @param testPlanName Testplan filename. This is used as title for the report tab, which helps
   *                     identify results if there are multiple Defensics steps in the Jenkins job.
   * @throws DefensicsRequestException If server responds with error
   * @throws IOException               If deleting the temporary report files in workspace fails
   * @throws InterruptedException      If deleting the temporary report files in workspace is
   *                                   interrupted.
   */
  public void publishResults(hudson.model.Run<?, ?> jenkinsRun, Run defensicsRun,
      FilePath workspace, String testPlanName)
      throws Exception {
    // HTML report publishing doesn't work properly without HTML Publisher version 1.20 or newer.
    // With 1.19, for example, one result is published ok, but two results don't show up properly.
    // Plugin manager class name needs to be checked because of a bug in Jenkins test harness
    // (https://issues.jenkins-ci.org/browse/JENKINS-48885) that causes pluginmanager not to have
    // any plugins when running Jenkins test harness, even though the plugins are there and usable.
    Plugin htmlPublisherPlugin = getHtmlPublisher();
    if (!getJenkinsPluginManager().getClass().getName()
        .equals("org.jvnet.hudson.test.TestPluginManager")
        && (htmlPublisherPlugin == null
        || htmlPublisherPlugin.getWrapper().getVersionNumber().compareTo(
        new VersionNumber("1.20")) < 0)) {
      logger.logError("Results can't be published without HTML Publisher Plugin 1.20 or newer. "
          + "Please install/update in Manage Jenkins > Manage Plugins. ");
      return;
    }

    logger.println("Downloading report.");
    workspace.mkdirs();
    final FilePath resultsDir = workspace.createTempDir("defensics-results", null);
    defensicsClient.saveResults(defensicsRun, resultsDir);

    HtmlReport report = null;
    try {
      report = new HtmlReport(resultsDir, defensicsRun.getId(), testPlanName);
      final ResultPublisher resultPublisher = getResultPublisher();
      resultPublisher.publishResults(jenkinsRun, defensicsRun, report, logger, workspace);
    } finally {
      if (report != null) {
        report.delete();
      }
    }
  }

  /**
   * Downloads and archives result package for the test run. Adds action to provide html link in the
   * build results.
   *
   * @param jenkinsRun   Jenkins run
   * @param defensicsRun Defensics run
   * @param testPlan Originally used test plan file to construct result-package file name
   * @throws Exception See {@link ApiService#saveResultPackage(FilePath, String, Run)
   *                   saveResultPackage} for possible exceptions
   */
  public void publishResultPackage(
      hudson.model.Run<?, ?> jenkinsRun,
      Run defensicsRun,
      FilePath testPlan
  )
      throws Exception {
    logger.println("Downloading result package.");
    final String resultFile = String
        .format("defensics-b%s-%s.zip", jenkinsRun.getId(), defensicsRun.getId());
    final FilePath filePath = new FilePath(jenkinsRun.getRootDir())
        .child(ResultPackageAction.URL_NAME);
    defensicsClient.saveResultPackage(filePath, resultFile, defensicsRun);
    ResultPackageAction resultPackageAction = jenkinsRun.getAction(ResultPackageAction.class);
    // Use only testplan name without .testplan extension in the link description to match
    // the tab wording in the HTML report view
    String description = testPlan.getName().replaceFirst(".testplan$", "");
    if (resultPackageAction == null) {
      resultPackageAction = new ResultPackageAction(resultFile, description);
    } else {
      resultPackageAction.addResultPackage(resultFile, description);
    }
    jenkinsRun.addOrReplaceAction(resultPackageAction);
  }

  /**
   * Handles that Defensics run is properly stopped if Jenkins run gets interrupted.
   */
  private void handleRunInterruption(Run run) {
    logger.println("Fuzzing was interrupted.");

    if (run == null) {
      // Not much to do since run either was not created at all or was completed and deleted
      // already.
      return;
    }

    try {
      // Update run to get latest information
      run = defensicsClient.getRun(run.getId());
      if (run == null) {
        return;
      }

      // We can't stop test run if suite isn't loaded so let's make sure it is
      final Optional<SuiteInstance> suiteMaybe = defensicsClient.getConfigurationSuite(run.getId());

      if (suiteMaybe.isPresent() && suiteMaybe.get().getState().equals(RunState.LOADING)) {
        logger.println("Suite loading is ongoing. Waiting for suite to load before unloading it.");
        waitForSuiteLoading(run);
      }

      // Stop job if it has been started. Starting and pausing runs might not be stoppable, but
      // they likely can transition to next states which can be stopped so include those to state
      // list. Interrupt handler stop-run retry mechanism should handle these cases.
      List<RunState> stoppableStates = Arrays.asList(
          RunState.STARTING,
          RunState.RUNNING,
          RunState.PAUSING,
          RunState.PAUSED
      );
      if (stoppableStates.contains(run.getState())) {
        final String runId = run.getId();
        logger.println("Stopping run.");
        try {
          defensicsClient.stopRun(runId);
        } catch (DefensicsRequestException e) {
          // Some suite states doesn't yet allow immediate stopping, giving 409 Conflict so retry
          // after brief delay. There's not yet apt exception for conflict state so check exception
          // message for 409.
          final boolean wasConflict = Optional.ofNullable(e.getCause())
              .map(Throwable::getMessage)
              .filter(message -> message.contains("409"))
              .isPresent();

          if (wasConflict) {
            logger.println("Couldn't yet stop run. Retrying.");
            TimeUnit.SECONDS.sleep(5);
            defensicsClient.stopRun(runId);
          }
        }
        int errorCounter = 0;
        while (errorCounter <= 3) {
          TimeUnit.SECONDS.sleep(1);
          run = defensicsClient.getRun(runId);
          switch (run.getState()) {
            case FATAL:
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
      if (exception.getMessage() != null) {
        logger.logError("Error message: " + exception.getMessage());
      }
    }
  }

  /**
   * HtmlPublisher getter to allow overriding in the unit tests.
   *
   * @return HtmlPublisher plugin
   */
  Plugin getHtmlPublisher() {
    return Jenkins.get().getPlugin("htmlpublisher");
  }

  /**
   * ResultPublisher getter to allow overriding in the unit tests.
   *
   * @return ResultPublisher
   */
  ResultPublisher getResultPublisher() {
    return new ResultPublisher();
  }

  /**
   * Jenkins plugin manager getter to allow overriding in the unit tests.
   *
   * @return PluginManager
   */
  PluginManager getJenkinsPluginManager() {
    return Jenkins.get().getPluginManager();
  }

  /**
   * ApiService getter to allow overriding unit tests.
   *
   * @param instanceConfiguration Defensics instance configuration
   * @param authenticationToken Authentication token to use for API requests
   * @return ApiService
   */
  ApiService getApiService(
      InstanceConfiguration instanceConfiguration,
      String authenticationToken
  ) {
    return new ApiService(
        instanceConfiguration.getUrl(),
        authenticationToken,
        instanceConfiguration.isCertificateValidationDisabled()
    );
  }

  /**
   * Polling intervals getter to allow overriding in the unit tests.
   *
   * @param jenkinsRun Jenkins run
   * @param launcher Launcher
   * @param logger Logger
   * @return PollingIntervals
   */
  PollingIntervals getPollingIntervals(
      hudson.model.Run<?, ?> jenkinsRun,
      Launcher launcher,
      Logger logger
  ) {
    return new PollingIntervals(jenkinsRun, launcher.getListener(), logger);
  }
}
