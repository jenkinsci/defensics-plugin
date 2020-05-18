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

package com.synopsys.defensics.api;

import com.synopsys.defensics.apiserver.client.DefensicsApiClient.DefensicsClientException;
import com.synopsys.defensics.apiserver.client.DefensicsJsonApiClient;
import com.synopsys.defensics.apiserver.model.Run;
import com.synopsys.defensics.apiserver.model.SettingCliArgs;
import com.synopsys.defensics.apiserver.model.SuiteInstance;
import com.synopsys.defensics.client.DefensicsRequestException;
import com.synopsys.defensics.client.UnsafeTlsConfigurator;
import com.synopsys.defensics.client.model.HtmlReport;
import hudson.FilePath;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.URI;
import java.nio.channels.ClosedByInterruptException;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.Optional;

public class ApiService {

  private final DefensicsJsonApiClient defensicsClient;
  private final URI apiBaseUrl;

  /**
   * Constructor for Job object. Job will be created but not started.
   */
  public ApiService(
      String defensicsInstanceUrl,
      String authenticationToken,
      boolean certificateValidationDisabled
  ) {
    // FIXME: Better slash handling, and client should check if /api/v1 is supplied already
    apiBaseUrl = defensicsInstanceUrl.endsWith("/")
        ? URI.create(defensicsInstanceUrl + "api/v1")
        : URI.create(defensicsInstanceUrl + "/api/v1");

    if (certificateValidationDisabled) {
      // Disable strict TLS checking if user has checked "Disable TLS checking".
      // Not preferred method, better would be to use TLS checking.
      defensicsClient = new DefensicsJsonApiClient(
          apiBaseUrl,
          authenticationToken,
          UnsafeTlsConfigurator::configureUnsafeTlsOkHttpClient
      );
    } else {
      // Default client - use JVMs TLS configuration and its certificate truststore.
      defensicsClient = new DefensicsJsonApiClient(
          apiBaseUrl,
          authenticationToken
      );
    }
  }

  /**
   * Checks API server health.
   *
   * @return true if server is up and healthy
   * @throws DefensicsRequestException if server responds with error
   */
  public boolean healthCheck() throws DefensicsRequestException, InterruptedException {
    try {
      return defensicsClient.healthcheck();
    } catch (DefensicsClientException e) {
      String message = "Unable to connect to Defensics API at address " + apiBaseUrl + ". "
          + "Please check you are using the correct token and Defensics API server is running. "
          + "Error message: "+ e.getCause().getMessage();
      mapAndThrow(e, message);
      // Should not reach this
      return false;
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
      mapAndThrow(e, "Failed to upload configuration");
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
      mapAndThrow(e, "Could not test configuration settings");
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
      mapAndThrow(e, "Failed to start run.");
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
      mapAndThrow(e, "Failed to get run");
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
      mapAndThrow(e, "Failed to stop run");
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
  public void saveResults(String runId, FilePath reportFolder)
      throws IOException, DefensicsRequestException, InterruptedException {

    try (InputStream cloudReportStream = defensicsClient.downloadReport(
          Collections.singletonList(runId),
          HtmlReport.Cloud.toString()
    )) {
      reportFolder.mkdirs();

      // Extract contents of the zip if report contains multiple files.
      // NOTE: There was code path to handle downloading HTML report as well but now only
      // zipped report is handled.
      reportFolder.unzipFrom(cloudReportStream);
    } catch (DefensicsClientException e) {
      mapAndThrow(e, "Failed to download results report.");
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
  public void saveResultPackage(FilePath resultFolder, String fileName, String runId)
      throws IOException, DefensicsRequestException, InterruptedException {
    try {
      final InputStream resultpackage = defensicsClient
          .downloadResultPackage(Collections.singletonList(runId));
      resultFolder.child(fileName).copyFrom(resultpackage);
    } catch (DefensicsClientException e) {
      mapAndThrow(e, "Failed to download result package.");
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
      mapAndThrow(e, "Could not create new Defensics run");
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
      return defensicsClient.getConfigurationSuite(id);
    } catch (DefensicsClientException e) {
      mapAndThrow(e, "Could not fetch suite information");
      // Should not reach this
      return Optional.empty();
    }
  }

  public void deleteRun(String runId)
      throws DefensicsRequestException, InterruptedException {
    try {
      defensicsClient.deleteRun(runId);
    } catch (DefensicsClientException e) {
      mapAndThrow(e, "Could not delete run");
    }
  }

  /**
   * Maps JSON:API client exceptions to either Jenkins' DefensicsRequestException or
   * to another exception types required e.g. in interrupted handling.
   * @param e Exception
   *
   * @param message
   * @throws DefensicsRequestException
   * @throws InterruptedException if operation was interrupted
   */
  private void mapAndThrow(DefensicsClientException e, String message)
      throws DefensicsRequestException, InterruptedException {
    // Check if cause was either interruption or some other failure
    if (
        e.getCause() instanceof InterruptedIOException
        || e.getCause() instanceof ClosedByInterruptException
    ) {
      throw new InterruptedException(e.getCause().getMessage());
    }
    if (e.getCause() instanceof InterruptedException) {
      throw (InterruptedException)e.getCause();
    }

    if (e.getMessage() != null) {
      message = message + ": " + e.getMessage();
    }
    throw new DefensicsRequestException(message, e);
  }
}
