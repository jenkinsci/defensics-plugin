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

package com.defensics.apiserver.client;

import com.defensics.apiserver.model.HealthCheckResult;
import com.defensics.apiserver.model.Run;
import com.defensics.apiserver.model.SettingCliArgs;
import com.defensics.apiserver.model.Suite;
import com.defensics.apiserver.model.SuiteInstance;
import com.defensics.apiserver.model.VersionInformation;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Interface for Defensics API client. Provides methods to do common Defensics tasks against
 * API server.
 */
public interface DefensicsApiClient {

  /**
   * Get detailed healthcheck information.
   *
   * @return Healthcheck information as a map
   * @throws DefensicsClientException if healthcheck information could not be fetched (eg. server
   *     down, TLS configuration wrong, wrong token).
   */
  Map<String, HealthCheckResult> getHealthChecks();

  /**
   * Fetches API server version information. In future may contain some other component version
   * information as well.
   *
   * @return VersionInformation containing server version.
   */
  Optional<VersionInformation> getServerVersion();

  /**
   * Uploads and assigns a Defensics testplan to the test configuration of run with given ID.
   *
   * @param configurationId Test configuration ID
   * @param testplanStream Defensics testplan file as a stream. Caller has to close the stream.
   */
  void uploadTestPlan(
      String configurationId,
      InputStream testplanStream
  );

  /**
   * Adds additional configuration settings for suite and monitor. The format is same as in
   * the Defensics command line (e.g. '--uri http://127.0.0.1:7000' for changing URI in HTTP server
   * suite).
   *
   * @param runId Run ID whose configuration is changed
   * @param settings Settings to add
   */
  void setTestConfigurationSettings(
      String runId,
      SettingCliArgs settings
  );

  /**
   * Get suite instance currently assigned to run configuration.
   * @param runId Run id
   * @return Suite instance assigned to configuration
   */
  Optional<SuiteInstance> getRunSuiteInstance(String runId);

  List<SuiteInstance> getSuiteInstances();

  /**
   * Creates new test run.
   *
   * @return new test run
   */
  Run createTestRun();

  /**
   * Gets run for given ID.
   *
   * @param runId Run ID
   * @return Run object
   */
  Optional<Run> getRun(String runId);

  /**
   * Removes the Run. Also removes related RunTestConfiguration and unloads any assigned suites
   *
   * @param runId Run ID
   */
  void deleteRun(String runId);

  /**
   * Starts Defensics run.
   *
   * @param runId Run ID
   */
  void startRun(String runId);

  /**
   * Stops given Defensics run.
   *
   * @param runId Run ID
   */
  void stopRun(String runId);

  /**
   * Downloads Defensics report of given type for given result IDs. Caller has to close the stream.
   *
   * @param resultId Run IDs for which to generate report
   * @param reportType Report type
   * @return Report as a stream
   */
  InputStream downloadReport(String resultId, String reportType);

  /**
   * Downloads Defensics result package for given result ID. Result package contains Defensics
   * result files in ZIP archive. Caller has to close the stream.
   *
   * @param resultId Result ID to include in the result package
   * @return Result package as an inputstream. Contains Zip-package
   */
  InputStream downloadResultPackage(String resultId);

  /**
   * Get single suite by the it's feature and version.
   *
   * @param suiteFeature the feature of suite to get
   * @param suiteVersion the version of suite to get
   * @return Suite
   */
  Optional<Suite> getSuite(String suiteFeature, String suiteVersion);

  /**
   * High level exception thrown when Defensics API client cannot do given operation succesfully.
   * Check the cause exception for further details.
   */
  class DefensicsClientException extends RuntimeException {
    public DefensicsClientException(String message) {
      super(message);
    }

    public DefensicsClientException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
