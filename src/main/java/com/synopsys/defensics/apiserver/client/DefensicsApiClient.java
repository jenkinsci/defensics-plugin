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

package com.synopsys.defensics.apiserver.client;

import com.synopsys.defensics.apiserver.model.Result;
import com.synopsys.defensics.apiserver.model.Run;
import com.synopsys.defensics.apiserver.model.RunTestConfiguration;
import com.synopsys.defensics.apiserver.model.Setting;
import com.synopsys.defensics.apiserver.model.SettingCliArgs;
import com.synopsys.defensics.apiserver.model.Suite;
import com.synopsys.defensics.apiserver.model.SuiteInstance;
import com.synopsys.defensics.apiserver.model.VersionInformation;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

/**
 * Interface for Defensics API client. Provides methods to do common Defensics tasks against
 * API server.
 */
public interface DefensicsApiClient {
  /**
   * Checks Defensics server status.
   *
   * @return Boolean true if server is healthy, false if not. Exception is thrown if connection to
   *     server was not possible.
   */
  boolean healthcheck();

  /**
   * Fetches API server version information. In future may contain some other component version
   * information as well.
   *
   * @return VersionInformation containing server version.
   */
  Optional<VersionInformation> getServerVersion();

  /**
   * Gets test configuration for given Run.
   *
   * @param runId id of the run to fetch configuration for
   * @return Test configuration
   */
  Optional<RunTestConfiguration> getRunConfiguration(
      String runId
  );

  /**
   * Uploads and assigns a Defensics testplan to the test configuration of run with given ID.
   *
   * @param configurationId Test configuration ID
   * @param testplanStream Defensics testplan file as a stream
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

  /**
   * Get suite instances.
   *
   * @return Suite instances
   */
  List<SuiteInstance> getSuiteInstances();

  /**
   * Get suite instance by id.
   *
   * @param suiteInstanceId Suite instance id
   * @return Suite instance
   */
  Optional<SuiteInstance> getSuiteInstance(String suiteInstanceId);

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
   * Get all runs.
   *
   * @return Runs
   */
  List<Run> getRuns();

  /**
   * Get all results
   *
   * @return List of results
   */
  List<Result> getResults();

  /**
   * Get all results matching given URL query string.
   *
   * @param query URL query string to add into URL. Actual functionality not yet done, and this can
   * change to some QuerySpec-style model.
   * @return Results matching given query
   */
  List<Result> getResults(String query);

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
   * Downloads Defensics report of given type for given run IDs. Caller has to close the stream.
   *
   * @param runId Run IDs for which to generate report
   * @param reportType Report type
   * @return Report as a stream
   */
  InputStream downloadReport(String runId, String reportType);

  /**
   * Downloads Defensics result package for given run ID. Result package contains Defensics
   * result files in ZIP archive. Caller has to close the stream.
   *
   * @param runId Run ID to include in the result package
   * @return Result package as an inputstream. Contains Zip-package
   */
  InputStream downloadResultPackage(String runId);

  /**
   * Pauses given Defensics run.
   * @param runId Run ID which is to be paused.
   */
  void pauseRun(String runId);

  /**
   * Resumes given Defensics run.
   * @param runId Run ID which is to be resumed.
   */
  void resumeRun(String runId);

  /**
   * Get all suites
   *
   * @return List of suites
   */
  List<Suite> getSuites();

  /**
   * Load a new suite instance of suite/version given in suiteId.
   *
   * @param suiteId Suite id
   * @return Suite instance.
   */
  SuiteInstance loadSuite(String suiteId);

  /**
   * Unload given suiteInstance
   *
   * @param suiteInstanceId Suite instance ID
   */
  void unloadSuiteInstance(String suiteInstanceId);

  /**
   * Assign given suite instance to run.
   *
   * @param suiteInstanceId Suite instance ID
   * @param runId Run ID where to assign the suite instance
   */
  void assignSuiteToRun(String suiteInstanceId, String runId);

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
