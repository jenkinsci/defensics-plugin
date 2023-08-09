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

package com.synopsys.defensics.api;

import com.synopsys.defensics.apiserver.client.DefensicsApiClient;
import com.synopsys.defensics.apiserver.client.DefensicsApiClient.DefensicsClientException;
import com.synopsys.defensics.apiserver.client.DefensicsApiV2Client;
import com.synopsys.defensics.apiserver.model.HealthCheckResult;
import com.synopsys.defensics.apiserver.model.Run;
import com.synopsys.defensics.apiserver.model.SettingCliArgs;
import com.synopsys.defensics.apiserver.model.Suite;
import com.synopsys.defensics.apiserver.model.SuiteInstance;
import com.synopsys.defensics.apiserver.model.VersionInformation;
import com.synopsys.defensics.client.DefensicsRequestException;
import com.synopsys.defensics.client.UnsafeTlsConfigurator;
import com.synopsys.defensics.client.model.HtmlReport;
import hudson.FilePath;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.channels.ClosedByInterruptException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang.exception.ExceptionUtils;

/**
 * Intermediate API service class between Jenkins job and Defensics client. Does things which client
 * doesn't yet do and could map some exception/messages to be more suitable for Jenkins.
 *
 * <p>NOTE: Exception handling is subject to change when client code is improved. Currently
 * DefensicsClientException is inspected and InterruptedExceptions are thrown separately to
 * handle job stopping more cleanly. DefensicsRequestException could be replaced with
 * DefensicsClientException if InterruptedExceptions are handled some way.
 */
public class ApiService {

  private final DefensicsApiClient defensicsClient;
  private final URI apiBaseUrl;

  /**
   * Constructor for Job object. Job will be created but not started.
   */
  public ApiService(
      String defensicsInstanceUrl,
      String authenticationToken,
      boolean certificateValidationDisabled
  ) {

    Consumer<HttpClient.Builder> clientConfigurator = builder -> {
      if (certificateValidationDisabled) {
        // Disable strict TLS checking if user has checked "Disable TLS checking".
        // Not preferred method, better would be to use TLS checking.
        UnsafeTlsConfigurator.configureUnsafeTlsOkHttpClient(builder);
      }
    };

    if (defensicsInstanceUrl.endsWith("api/v2")) {
      apiBaseUrl = URI.create(defensicsInstanceUrl);
    } else {
      // Add correct API version if only base address is given
      apiBaseUrl = defensicsInstanceUrl.endsWith("/")
          ? URI.create(defensicsInstanceUrl + "api/v2")
          : URI.create(defensicsInstanceUrl + "/api/v2");
    }
    defensicsClient = new DefensicsApiV2Client(
        apiBaseUrl,
        authenticationToken,
        clientConfigurator
    );
  }

  /**
   * Alternative constructor for unit testing. Takes already created DefensicsClient.
   *
   * @param defensicsClient Defensics client
   * @param apiBaseUrl API server base URL containing the trailing "/api/v2"
   */
  ApiService(DefensicsApiClient defensicsClient, URI apiBaseUrl) {
    this.defensicsClient = defensicsClient;
    this.apiBaseUrl = apiBaseUrl;
  }

  /**
   * Checks API server health.
   *
   * @return true if server is up and healthy
   * @throws DefensicsRequestException if server responds with error
   */
  public boolean healthCheck() throws DefensicsRequestException, InterruptedException {
    try {
      return getFailingHealthChecks().isEmpty();
    } catch (DefensicsClientException e) {
      mapAndThrow(e);
      // Should not reach this
      return false;
    }
  }

  /**
   * Get server health check status and returns unhealthy check results.
   *
   * @return Unhealthy check information
   * @throws DefensicsRequestException if health check information could not be fetched
   * @throws InterruptedException If processing was interrupted
   */
  public Map<String, HealthCheckResult> getFailingHealthChecks()
      throws DefensicsRequestException, InterruptedException {
    try {
      return defensicsClient.getHealthChecks()
          .entrySet()
          .stream()
          .filter(entry -> !entry.getValue().isHealthy())
          .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    } catch (DefensicsClientException e) {
      mapAndThrow(e);
      return null;
    }
  }

  /**
   * Upload a testplan file to the API server.
   *
   * @param configurationId Test configuration id
   * @param testplan        Testplan file
   * @throws IOException               if there's an issue communicating with the server or reading
   *                                   test plan file
   * @throws DefensicsRequestException if server responds with error
   * @throws InterruptedException      if reading test plan file is interrupted
   */
  public void uploadTestPlan(String configurationId, FilePath testplan)
      throws IOException, DefensicsRequestException, InterruptedException {
    try (final InputStream testplanStream = testplan.read()) {
      defensicsClient.uploadTestPlan(configurationId, testplanStream);
    } catch (DefensicsClientException e) {
      mapAndThrow(e);
    }
  }

  /**
   * Sends test configuration settings using CLI format.
   *
   * @param runId    configuration id
   * @param settings settings using Defensics CLI format
   * @throws DefensicsRequestException if server responds with error
   */
  public void setTestConfigurationSettings(String runId, String settings)
      throws DefensicsRequestException, InterruptedException {
    final SettingCliArgs settingCliArgs = new SettingCliArgs();
    settingCliArgs.setArgs(settings);

    try {
      defensicsClient.setTestConfigurationSettings(
          runId,
          settingCliArgs
      );
    } catch (DefensicsClientException e) {
      mapAndThrow(e);
    }
  }

  /**
   * Starts testrun using previously created test configuration.
   *
   * @param runId ID for the started run
   * @throws DefensicsRequestException if server responds with error
   */
  public void startRun(String runId)
      throws DefensicsRequestException, InterruptedException {
    try {
      defensicsClient.startRun(runId);
    } catch (DefensicsClientException e) {
      mapAndThrow(e);
    }
  }


  /**
   * Get individual run.
   *
   * @param runId run id for the run to get
   * @return Run object or null if run can't be found
   */
  public Run getRun(String runId)
      throws DefensicsRequestException, InterruptedException {
    try {
      return defensicsClient.getRun(runId)
          .orElseThrow(
              () -> new DefensicsRequestException("Could not find Defensics run " + runId));
    } catch (DefensicsClientException e) {
      mapAndThrow(e);
      // Should not reach this
      return null;
    }
  }

  /**
   * Makes a request to stop the test run. Test run must be running or paused.
   *
   * @param runId run id
   */
  public void stopRun(String runId)
      throws DefensicsRequestException, InterruptedException {
    try {
      defensicsClient.stopRun(runId);
    } catch (DefensicsClientException e) {
      mapAndThrow(e);
    }
  }

  /**
   * Downloads the report for this job from Defensics.
   *
   * @throws IOException               if there's an issue communicating with the server or writing
   *                                   report file
   * @throws DefensicsRequestException if server responds with error
   * @throws InterruptedException      if reading test plan file is interrupted
   */
  public void saveResults(Run run, FilePath reportFolder)
      throws IOException, DefensicsRequestException, InterruptedException {

    try (InputStream cloudReportStream = defensicsClient.downloadReport(
          run.getResultId(),
          HtmlReport.Cloud.toString()
    )) {
      reportFolder.mkdirs();

      // Extract contents of the zip if report contains multiple files.
      // NOTE: There was code path to handle downloading HTML report as well but now only
      // zipped report is handled.
      reportFolder.unzipFrom(cloudReportStream);
    } catch (DefensicsClientException e) {
      mapAndThrow(e);
    }
  }

  /**
   * Download the result package (zip-file) and saves it to provided result folder.
   *
   * @throws IOException               if there's an issue communicating with the server or writing
   *                                   report file
   * @throws DefensicsRequestException if server responds with error
   * @throws InterruptedException      if FilePath actions fail
   */
  public void saveResultPackage(FilePath resultFolder, String fileName, Run run)
      throws IOException, DefensicsRequestException, InterruptedException {
    try {
      final InputStream resultpackage = defensicsClient.downloadResultPackage(run.getResultId());
      resultFolder.child(fileName).copyFrom(resultpackage);
    } catch (DefensicsClientException e) {
      mapAndThrow(e);
    }
  }

  /**
   * Creates new empty Defensics run. This need to be configured by uploading testplan (later on
   * other configuration options can be available).
   *
   * @return New Defensics run object
   * @throws DefensicsRequestException if run cannot be created in the Defensics server
   */
  public Run createNewRun() throws DefensicsRequestException, InterruptedException {
    try {
      return defensicsClient.createTestRun();
    } catch (DefensicsClientException e) {
      mapAndThrow(e);
      // Should not reach this
      return null;
    }
  }

  /**
   * Get suite information for given Defensics run.
   *
   * @param id Defensics run ID
   * @return Suite instance information or empty optional if suite was not found
   * @throws DefensicsRequestException if suite information could not be fetched from Defensics
   *                                   server
   */
  public Optional<SuiteInstance> getConfigurationSuite(String id)
      throws DefensicsRequestException, InterruptedException {
    try {
      return defensicsClient.getRunSuiteInstance(id);
    } catch (DefensicsClientException e) {
      mapAndThrow(e);
      // Should not reach this
      return Optional.empty();
    }
  }

  public void deleteRun(String runId)
      throws DefensicsRequestException, InterruptedException {
    try {
      defensicsClient.deleteRun(runId);
    } catch (DefensicsClientException e) {
      mapAndThrow(e);
    }
  }

  /**
   * Get Suite for the given run+suite-instance. Suite contains full suite name and some other
   * information which cannot be found from suite-instance. Note: This eats client errors so refine
   * error handling if suite information is necessary for run to proceed. Now suite is just used to
   * print suite name and version.
   *
   * @param run Run having assigned suite instance
   * @return Suite if found, otherwise empty optional.
   */
  public Optional<Suite> getSuiteInformationForRun(Run run) {
    try {
      final SuiteInstance suiteInstance = defensicsClient.getRunSuiteInstance(run.getId())
          .orElseThrow(() -> new IllegalStateException("Suite instance not found"));
      return defensicsClient.getSuite(
          suiteInstance.getSuiteFeature(),
          suiteInstance.getSuiteVersion()
      );
    } catch (DefensicsClientException | IllegalStateException e) {
      return Optional.empty();
    }
  }

  /**
   * Returns API server version as a string, or empty optional if request fails for some reason.
   *
   * @return API server version (same as monitorVersion)
   */
  public Optional<String> getServerVersion() {
    try {
      return defensicsClient.getServerVersion().map(VersionInformation::getMonitorVersion);
    } catch (DefensicsClientException e) {
      return Optional.empty();
    }
  }

  /**
   * Maps JSON:API client exceptions to either Jenkins' DefensicsRequestException or
   * to another exception types required e.g. in interrupted handling. Exception message is
   * directly the cause exception's message.
   *
   * @param e Exception
   * @throws DefensicsRequestException mapped from DefensicsClientException
   * @throws InterruptedException if inner exception was interrupted
   */
  private void mapAndThrow(DefensicsClientException e)
      throws DefensicsRequestException, InterruptedException {
    // Call mapAndThrow using default renderer, i.e. just return exception's message
    mapAndThrow(e, Throwable::getMessage);
  }

  /**
   * Maps JSON:API client exceptions to either Jenkins' DefensicsRequestException or
   * to another exception types required e.g. in interrupted handling.
   *
   * @param e Exception
   * @param messageRenderer Function to render error message. Receives exception as an argument
   * @throws DefensicsRequestException mapped from DefensicsClientException
   * @throws InterruptedException if inner exception was interrupted
   */
  private void mapAndThrow(
      DefensicsClientException e,
      Function<DefensicsClientException, String> messageRenderer
  ) throws DefensicsRequestException, InterruptedException {
    final Exception cause = (Exception)e.getCause();

    // Check if there was user interruption, and if yes, map to InterruptedException
    if (cause != null) {
      boolean jobInterruptedByUser =
          (ExceptionUtils.indexOfType(cause, InterruptedIOException.class) >= 0
              || ExceptionUtils.indexOfType(cause, ClosedByInterruptException.class) >= 0
              || ExceptionUtils.indexOfType(cause, InterruptedException.class) >= 0);

      // SocketTimeoutException comes from HttpClient when e.g. readTimeout is met so
      // this shouldn't come from user actions and should be classified as build fail.
      if (cause instanceof SocketTimeoutException) {
        jobInterruptedByUser = false;
      }

      if (jobInterruptedByUser) {
        throw new InterruptedException(e.getCause().getMessage());
      }
    }

    String message = messageRenderer.apply(e);

    // Map from DefensicsClientException and include inner exception if present
    if (cause != null) {
      throw new DefensicsRequestException(message, cause);
    }

    throw new DefensicsRequestException(message);
  }

  /**
   * Returns formatted multiline string block about unhealthy health checks to be used in warnings.
   *
   * @param failingHealthChecks Unhealthy health checks
   * @return Unhealthy health check(s) information - check name and message. Format is unstable.
   */
  public static String formatUnhealthyHealthCheckLines(
      Map<String, HealthCheckResult> failingHealthChecks
  ) {
    final String healthCheckLines = failingHealthChecks.entrySet()
        .stream()
        .map(e ->
            String.format("Health check '%s'%s",
                e.getKey(),
                e.getValue().getMessage().isEmpty() ? "" : ", message: " + e.getValue().getMessage()
            )
        )
        .collect(Collectors.joining("\n"));
    return healthCheckLines;
  }
}
