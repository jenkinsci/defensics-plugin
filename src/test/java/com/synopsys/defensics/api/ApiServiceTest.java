/*
 * Copyright © 2020-2023 Synopsys, Inc.
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;

import com.synopsys.defensics.apiserver.client.DefensicsApiV2Client;
import com.synopsys.defensics.apiserver.model.Run;
import com.synopsys.defensics.apiserver.model.RunState;
import com.synopsys.defensics.apiserver.model.RunVerdict;
import com.synopsys.defensics.client.DefensicsRequestException;
import com.synopsys.defensics.jenkins.test.utils.DefensicsMockServer;
import hudson.FilePath;
import java.io.File;
import java.net.URI;
import java.time.Duration;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockserver.integration.ClientAndServer;

public class ApiServiceTest {

  private static final String SUITE_SETTINGS = "--suite.setting=thisIsFakeSetting";
  private static ClientAndServer mockServer;
  private static final String DEFENSICS_URL = "http://127.0.0.1:1080/";
  private static final String TOKEN = "test-token";
  private static final boolean CERTIFICATE_VALIDATION_DISABLED = false;
  private ApiService api;

  @Before
  public void setUp() {
    mockServer = ClientAndServer.startClientAndServer(1080);
    final DefensicsMockServer defensicsMockServer = new DefensicsMockServer(
        true,
        RunVerdict.PASS,
        RunState.STARTING
    );
    defensicsMockServer.initServer(mockServer);
    api = new ApiService(DEFENSICS_URL, TOKEN, CERTIFICATE_VALIDATION_DISABLED);
  }

  @After
  public void tearDown() {
    DefensicsMockServer.stopMockServer(mockServer);
  }

  @Test
  public void testTestConnectionSuccess() throws Exception {
    api.healthCheck(); //causes Exception if failing
  }

  @Test
  public void testTestConnectionFailConnection() throws Exception {
    try {
      api = new ApiService(
          "http://invalid.invalid", TOKEN, CERTIFICATE_VALIDATION_DISABLED);
      api.healthCheck();
      fail("Test connection with false url did not fail");
    } catch (DefensicsRequestException exception) {
      assertThat(
          exception.getMessage(), containsString("invalid.invalid"));
    }
  }

  @Ignore("FIXME: API client should return response even on 500 error")
  @Test
  public void testTestConnectionFailAuth() throws Exception {
    try {
      api = new ApiService(
          DEFENSICS_URL, "wrong-token", CERTIFICATE_VALIDATION_DISABLED);
      api.healthCheck();
      fail("Test connection authentication did not return DefensicsRequestException");
    } catch (DefensicsRequestException exception) {
      final String apiAddress = "http://127.0.0.1:1080/api/v2/healthcheck";

      assertThat(
          exception.getMessage(),
          is(equalTo("Unable to connect Defensics server health check at address " + apiAddress
              + ". "
              + "Please check you are using the "
              + "correct token and Defensics API server is running. "
              + "Could not get healthcheck. HTTP status code: 401, message: Unauthorized. "
              + "No authentication credentials found in request."
              )));
    }
  }

  @Test
  public void testUploadTestplan() throws Exception {
    File file = new File("src/test/resources/com/synopsys/defensics/client/test.testplan");
    api.uploadTestPlan(DefensicsMockServer.RUN_ID, new FilePath(file));
  }

  @Test
  public void testSetConfigurationSettings()
      throws DefensicsRequestException, InterruptedException {
    api.setTestConfigurationSettings(DefensicsMockServer.RUN_ID, SUITE_SETTINGS);
  }

  @Test
  public void testGetRun() throws Exception {
    Run run = api.getRun(DefensicsMockServer.RUN_ID);
    assertThat(run.getId(), is(equalTo(DefensicsMockServer.RUN_ID)));
    assertThat(run.getState(), is(equalTo(RunState.STARTING)));
  }

  @Test
  public void testFetchJobReport() throws Exception {
    TemporaryFolder temporaryFolder = new TemporaryFolder();
    temporaryFolder.create();
    FilePath resultFolder = new FilePath(temporaryFolder.getRoot());
    Run run = api.getRun(DefensicsMockServer.RUN_ID);
    api.saveResults(run, resultFolder);
    assertThat(resultFolder.exists(), is(equalTo(true)));
    assertThat(resultFolder.child("report.html").exists(), is(equalTo(true)));
  }

  /**
   * Test following things:
   *
   * 1) Client timeout setting works
   * 2) Exception thrown from exceeding timeouts is DefensicsRequestException instead of
   *   InterruptedException so that'd make build to FAIL instead of ABORTED.
   *
   *  Note: Timeouts aren't currently used with Java HTTP client as most of the timeout settings have
   *  moved to be request-specific instead of global setting. This test now just tests that it's
   *  possible to set that one global connectTimeout if needed.
   */
  @Test
  public void testClientTimeoutHandling() {
    // Stops server so there isn't anything responding to this connection attempt. This is somewhat
    // brittle
    DefensicsMockServer.stopMockServer(mockServer);
    final URI apiBaseUri = URI.create(DEFENSICS_URL);
    final DefensicsApiV2Client clientWithShortTimeout = new DefensicsApiV2Client(
        apiBaseUri.resolve("/api/v2"),
        TOKEN,
        builder -> builder.connectTimeout(Duration.ofMillis(1))
    );
    final ApiService apiService = new ApiService(clientWithShortTimeout, apiBaseUri);
    final DefensicsRequestException defensicsRequestException = assertThrows(
        DefensicsRequestException.class,
        // Mock server is configured to have 5 ms delay for create-run request
        apiService::createNewRun
    );

    assertThat(
        defensicsRequestException.getMessage(),
        containsString("Could not create test run")
    );
    // timeout wording varies a bit between test environments
    assertThat(
        defensicsRequestException.getMessage(),
        anyOf(
            containsString("timeout"),
            containsString("timed out")
        )
    );
  }
}
