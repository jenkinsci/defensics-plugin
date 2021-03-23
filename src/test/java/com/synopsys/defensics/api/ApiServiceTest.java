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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

import com.synopsys.defensics.apiserver.model.Run;
import com.synopsys.defensics.apiserver.model.RunState;
import com.synopsys.defensics.client.DefensicsRequestException;
import com.synopsys.defensics.jenkins.test.utils.DefensicsMockServer;
import hudson.FilePath;
import java.io.File;
import org.junit.After;
import org.junit.Before;
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
  public void setUp() throws Exception {
    mockServer = ClientAndServer.startClientAndServer(1080);
    final DefensicsMockServer defensicsMockServer = new DefensicsMockServer(
        true,
        "PASS",
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
          "http://invalid_url.not.found", TOKEN, CERTIFICATE_VALIDATION_DISABLED);
      api.healthCheck();
      fail("Test connection with false url did not fail");
    } catch (DefensicsRequestException exception) {
      assertThat(
          exception.getMessage(), containsString("invalid_url.not.found"));
    }
  }

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
          is(equalTo("Unable to connect Defensics server healthcheck at address " + apiAddress
              + ". "
              + "Please check you are using the "
              + "correct token and Defensics API server is running. "
              + "HTTP status code: 401, message: Unauthorized. No authentication credentials found in request."
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
}
