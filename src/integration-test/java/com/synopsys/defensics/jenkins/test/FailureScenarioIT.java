/*
 * Copyright © 2020-2022 Synopsys, Inc.
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

package com.synopsys.defensics.jenkins.test;

import static com.synopsys.defensics.jenkins.test.utils.Constants.CERTIFICATE_VALIDATION_DISABLED;
import static com.synopsys.defensics.jenkins.test.utils.Constants.NAME;
import static com.synopsys.defensics.jenkins.test.utils.Constants.PIPELINE_ERROR_TEXT;
import static com.synopsys.defensics.jenkins.test.utils.Constants.SETTING_FILE_PATH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import com.synopsys.defensics.api.ApiService;
import com.synopsys.defensics.apiserver.client.DefensicsApiClient;
import com.synopsys.defensics.apiserver.client.DefensicsApiV2Client;
import com.synopsys.defensics.apiserver.model.SuiteInstance;
import com.synopsys.defensics.client.DefensicsRequestException;
import com.synopsys.defensics.client.UnsafeTlsConfigurator;
import com.synopsys.defensics.jenkins.result.HtmlReportPublisherTarget.HtmlReportAction;
import com.synopsys.defensics.jenkins.result.ResultPackageAction;
import com.synopsys.defensics.jenkins.test.utils.CredentialsUtil;
import com.synopsys.defensics.jenkins.test.utils.HttpSutRule;
import com.synopsys.defensics.jenkins.test.utils.JenkinsJobUtils;
import com.synopsys.defensics.jenkins.test.utils.ProjectUtils;
import htmlpublisher.HtmlPublisherTarget.HTMLAction;
import hudson.EnvVars;
import hudson.model.Result;
import hudson.model.Slave;
import hudson.model.queue.QueueTaskFuture;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Assume;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * End-to-end tests testing different (failure) modes.
 *
 * <p>These tests need externally running API server which has HTTP suite 4.13.0 installed.
 * Now this class is run manually by setting the configuration values in this class or by passing
 * values with system properties. See code for currently supported system properties.
 * </p>
 * <p>This class could be renamed and moved outside from integration tests to e.g. e2e-tests.</p>
 */
public class FailureScenarioIT {

  /** Tests are not run until this is true. */
  private static final boolean hasRequiredDependencies = Boolean.parseBoolean(
      System.getProperty("DEFENSICS_RUN_E2E_TESTS", "false")
  );

  /** API Server address. */
  private static final URI API_SERVER_URL = URI.create(
      System.getProperty("DEFENSICS_API_URL", "https://127.0.0.1:3150" )
  );

  /**
   * API token for API server. Set this when starting tests.
   */
  private static final String API_TOKEN = System.getProperty("DEFENSICS_API_TOKEN", null);

  /** JUnit rule to start and stop stub HTTP server in given port. */
  @Rule
  public HttpSutRule httpSutRule = new HttpSutRule(7000);

  /** Used SUT address. */
  private final URI SUT_URI = httpSutRule.getAddress();

  /**
   * Set to true if API server has been started with --enable-script-execution.
   * Used to determine if error reporting tests can be run.
   */
  private static final boolean API_SERVER_HAS_ENABLED_EXTERNAL_INSTRUMENTATION = false;

  private final String defaultPipelineScript = createPipelineScript(
      NAME,
      SETTING_FILE_PATH,
      String.format("--uri %s", SUT_URI),
      false
  );

  private String credentialsId;
  private int initialSuiteInstanceCount = -1;

  @Rule
  public final JenkinsRule jenkinsRule = new JenkinsRule();
  private WorkflowJob project;
  private ApiUtils apiUtils;

  @ClassRule
  public static final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Before
  public void checkRequisites() {
    Assume.assumeTrue("Test needs external services running", hasRequiredDependencies);
  }

  /**
   * Creates Jenkins pipeline script with given values.
   * @param defensicsInstance Defensics instance name
   * @param configurationFilePath Configuration file path
   * @param configurationOverride Setting override or empty string.
   * @return Jenkins pipeline script
   */
  private static String createPipelineScript(
      String defensicsInstance,
      String configurationFilePath,
      String configurationOverride,
      boolean saveResultPackage
  ) {
    return "node {\n"
        + "  stage('Build') {\n"
        + "    try {\n"
        + "     defensics(\n"
        + "       defensicsInstance: '" + defensicsInstance + "', "
        + "       configurationFilePath: '" + configurationFilePath + "', "
        + "       configurationOverrides: '" + configurationOverride + "', "
        + "       saveResultPackage: " + saveResultPackage
        + "     )\n"
        + "     echo \"Step 2: Print line after fuzz job\"\n"
        + "    } catch (error) {\n"
        + "      echo \"" + PIPELINE_ERROR_TEXT + "\";\n"
        + "      throw error\n"
        + "    }\n"
        + "  }\n"
        + "}\n";
  }

  /** Setup method. */
  @Before
  public void setup() throws Exception {
    EnvironmentVariablesNodeProperty prop = new EnvironmentVariablesNodeProperty();
    EnvVars env = prop.getEnvVars();
    env.put("DEFENSICS_MAX_POLLING_INTERVAL", "1");
    jenkinsRule.jenkins.getGlobalNodeProperties().add(prop);

    credentialsId = CredentialsUtil.createValidCredentials(jenkinsRule.jenkins, API_TOKEN);
    project = jenkinsRule.createProject(WorkflowJob.class);

    // Use ApiUtils for now since it's used to fetch only suite-instance count. APIv2 client has
    // already this functionality so this can be removed when APIv1 is removed.
    apiUtils = new ApiUtils(API_SERVER_URL.resolve("/api/v2"), API_TOKEN);
  }

  /**
   * Test whole build and that that suite is unloaded after test run.
   */
  @Test
  public void testRun() throws Exception {
    initialSuiteInstanceCount = apiUtils.getSuiteInstances().size();
    String pipelineScript = createPipelineScript(
        NAME,
        SETTING_FILE_PATH,
        String.format("--uri %s", SUT_URI),
        true
    );
    setupProject(pipelineScript);

    WorkflowRun run = project.scheduleBuild2(0).get();
    dumpLogs(run);

    // Check that suite name and version is printed. Suite version may vary so allow some variation.
    assertThat(
        logHas(run, Pattern.compile("Waiting for HTTP Server [0-9.-a-z]+ suite to load")),
        is(true)
    );

    assertThat(
        logHas(run, Pattern.compile("Defensics server version: [2][0-1][0-9][0-9].*")),
        is(true)
    );

    final int expectedCaseCount = 101;
    String expectedStatusLine = String.format(
        "[Defensics] 100.0%% (%d/%d) of tests run. All passed.",
        expectedCaseCount,
        expectedCaseCount
    );
    assertThat(logHas(run, expectedStatusLine), is(true));
    assertThat(logHas(run, "(0/0) of tests run"), is(false));

    checkRunOkAndReportPresent(run);
    checkApiServerResourcesAreCleaned();
    checkResultPackagePresent(run, SETTING_FILE_PATH);
  }

  /**
   * Test whole build on a separate node and that that suite is unloaded after test run. Note:
   * This test is still running in same machine so it doesn't test all aspects, eg. remote
   * filesystem handling.
   */
  @Test
  public void testRun_runOnSeparateNode() throws Exception {
    initialSuiteInstanceCount = apiUtils.getSuiteInstances().size();

    Slave slave = jenkinsRule.createSlave();
    // Separate node has different workspace (in same computer in these tests, though) so refer
    // the source testplan with the absolute path
    Path absoluteTestplanPath = Paths.get(
        "./src/integration-test/resources/com/synopsys/defensics/jenkins/test/utils/http.testplan"
    ).toAbsolutePath();

    assertThat(Files.exists(absoluteTestplanPath), is(true));

    String pipelineScript = createPipelineScript(
        NAME,
        absoluteTestplanPath.toString(),
        String.format("--uri %s", SUT_URI),
        false
    );

    String scriptWithNode = pipelineScript.replaceFirst(
        "node",
        String.format("node('%s')", slave.getNodeName())
    );
    // Ensure that replace did something
    assertThat(scriptWithNode, is(not(equalTo(pipelineScript))));

    setupProject(scriptWithNode);

    WorkflowRun run = project.scheduleBuild2(0).get();

    checkRunOkAndReportPresent(run);
    checkApiServerResourcesAreCleaned();
  }

  /**
   * Test that failure count is reported.
   */
  @Test
  public void testRun_hasInstrumentationFailure() throws Exception {
    initialSuiteInstanceCount = apiUtils.getSuiteInstances().size();
    // Configure run use instrumentation: Since we have dummy HTTP server, easiest is to abuse
    // status filter to expect 500 instead of normal 1xx 2xx 3xx.
    String instrumentationString = String.join(" ", Arrays.asList(
        "--index 1",
        "--instrument",
        "--http-status-filter 500",
        "--instrumentation-stop-limit 1"
    ));

    String pipelineScript = createPipelineScript(
        NAME,
        SETTING_FILE_PATH,
        String.format("--uri %s %s", SUT_URI, instrumentationString),
        true
    );

    setupProject(pipelineScript);

    WorkflowRun run = project.scheduleBuild2(0).get();
    dumpLogs(run);

    assertThat(logHas(run, "100.0% (1/1) of tests run. Some FAILED."), is(true));
    assertThat(logHas(run, "Failures: 1"), is(true));
    assertThat(logHas(run, "Fuzzing completed with verdict FAIL and 1 failures."), is(true));

    checkRunAndReportPresent(run, Result.FAILURE, true);
    checkApiServerResourcesAreCleaned();
  }


  /**
   * Tests that plugin can handle cases where suite is reloaded after sending reloadRequired
   * setting.
   */
  @Test
  public void testRun_hasSuiteReload() throws Exception {
    initialSuiteInstanceCount = apiUtils.getSuiteInstances().size();
    String instrumentationString = String.join(" ", Arrays.asList(
        "--index 1-100",
        "--tg-text off" // Anomaly control change requires suite reload
    ));

    String pipelineScript = createPipelineScript(
        NAME,
        SETTING_FILE_PATH,
        String.format("--uri %s %s", SUT_URI, instrumentationString),
        false
    );
    setupProject(pipelineScript);

    WorkflowRun run = project.scheduleBuild2(0).get();
    dumpLogs(run);

    checkRunOkAndReportPresent(run);
    checkApiServerResourcesAreCleaned();
  }

  /**
   * Test that Jenkins runs report at least WARNING if SUT URI was wrong.
   */
  @Test
  public void testRun_wrongUri() throws Exception {
    final String wrongSutUri = "http://non-routable.invalid:9999";
    String pipelineScript = createPipelineScript(
        NAME,
        SETTING_FILE_PATH,
        String.format("--uri %s", wrongSutUri),
        true
    );
    initialSuiteInstanceCount = apiUtils.getSuiteInstances().size();
    setupProject(pipelineScript);

    WorkflowRun run = project.scheduleBuild2(0).get();
    dumpLogs(run);

    String expectedMessage = "Suite error: Unknown host 'non-routable.invalid'";
    assertThat(logHas(run, expectedMessage), is(true));
    assertThat(run.getResult(), is(equalTo(Result.FAILURE)));

    checkApiServerResourcesAreCleaned();
    checkRunAndReportPresent(run, Result.FAILURE, true);
    checkResultPackagePresent(run, SETTING_FILE_PATH);
  }

  /**
   * Test that Jenkins runs report at least WARNING if SUT port was wrong. NOTE: Unreliable test
   * if host has a service using this port.
   */
  @Test
  public void testRun_wrongPort() throws Exception {
    final String wrongSutUri = "http://127.0.0.1:9999";
    String pipelineScript = createPipelineScript(
        NAME,
        SETTING_FILE_PATH,
        String.format("--uri %s", wrongSutUri),
        true
    );
    initialSuiteInstanceCount = apiUtils.getSuiteInstances().size();
    setupProject(pipelineScript);

    WorkflowRun run = project.scheduleBuild2(0).get();
    dumpLogs(run);

    String expectedMessage = "Suite error: Could not connect to 127.0.0.1:9999"
        + ", please check target address and port.";
    assertThat(logHas(run, expectedMessage), is(true));
    assertThat(run.getLog(100).contains(PIPELINE_ERROR_TEXT), is(true));
    assertThat(run.getResult(), is(equalTo(Result.FAILURE)));

    // NOTE: In this case first connection failed so there's no results but later on suite error
    // can occur at later stage and we'd likely want to have report for already executed cases
    // if report can be generated
    checkRunAndReportPresent(run, Result.FAILURE, true);
    checkResultPackagePresent(run, SETTING_FILE_PATH);
    checkApiServerResourcesAreCleaned();
  }

  @Test
  public void testRun_emptyTestplan() throws Exception {
    final File emptyFile = temporaryFolder.newFile();

    final String pipelineScriptEmptyTestplan = createPipelineScript(
        NAME,
        emptyFile.getAbsolutePath(),
        "",
        false
    );
    initialSuiteInstanceCount = apiUtils.getSuiteInstances().size();
    setupProject(pipelineScriptEmptyTestplan);

    WorkflowRun run = project.scheduleBuild2(0).get();
    dumpLogs(run);
    assertThat(run.getResult(), is(equalTo(Result.FAILURE)));
    assertThat(logHas(run, "Not valid configuration file"), is(true));
    checkNoReport(run);
    checkApiServerResourcesAreCleaned();
  }

  /**
   * Test that API Server response is shown if testplan has external script but this is disabled
   * in API Server.
   */
  @Test
  public void testRun_hasForbiddenExternalInstrumentation() throws Exception {
    Assume.assumeFalse(
        "API server should have disabled external instrumentation running for this test",
        API_SERVER_HAS_ENABLED_EXTERNAL_INSTRUMENTATION
    );
    initialSuiteInstanceCount = apiUtils.getSuiteInstances().size();
    final String pipelineScriptDefiningExternalInstrumentation = createPipelineScript(
        NAME,
        SETTING_FILE_PATH,
        "--exec-instrument echo 1",
        false
    );
    setupProject(pipelineScriptDefiningExternalInstrumentation);

    WorkflowRun run = project.scheduleBuild2(0).get();

    dumpLogs(run);
    assertThat(run.getResult(), is(equalTo(Result.FAILURE)));
    final String expectedError = "There are settings that may cause security risk. If you want to "
        + "use them please check manual how to enable script execution.";
    assertThat(logHas(run, expectedError), is(true));
    checkNoReport(run);
    checkApiServerResourcesAreCleaned();
  }

  /**
   * Test that job abort is handled cleanly when Fuzz job is on some of these states (current
   * triggering mechanism doesn't yet allow aborting in exact given step):
   * 1) Run is being created
   * 2) Test plan is uploaded
   * 3) Suite is loading
   */
  @Test
  public void testAbortJob_onRunCreation() throws Exception {
    initialSuiteInstanceCount = apiUtils.getSuiteInstances().size();
    ProjectUtils.setupProject(
        jenkinsRule,
        project,
        NAME,
        API_SERVER_URL.toString(),
        CERTIFICATE_VALIDATION_DISABLED,
        credentialsId,
        SETTING_FILE_PATH);

    project.setDefinition(new CpsFlowDefinition(defaultPipelineScript, true));

    // Schedule build
    final QueueTaskFuture<WorkflowRun> runFuture = project.scheduleBuild2(0);

    Thread.sleep(100);

    final WorkflowRun lastBuild = project.getLastBuild();
    JenkinsJobUtils.triggerAbortOnLogLine(lastBuild, "Creating");

    WorkflowRun run = runFuture.get();
    dumpLogs(run);

    checkRunAbortedCleanly(run);
    checkNoReport(run);
    checkApiServerResourcesAreCleaned();
  }

  /**
   * Test that job abort is handled cleanly when suite is being loaded
   */
  @Test
  public void testAbortJob_onFuzzSuiteLoading() throws Exception {
    initialSuiteInstanceCount = apiUtils.getSuiteInstances().size();
    ProjectUtils.setupProject(
        jenkinsRule,
        project,
        NAME,
        API_SERVER_URL.toString(),
        CERTIFICATE_VALIDATION_DISABLED,
        credentialsId,
        SETTING_FILE_PATH);

    project.setDefinition(new CpsFlowDefinition(defaultPipelineScript, true));

    // Schedule build
    final QueueTaskFuture<WorkflowRun> runFuture = project.scheduleBuild2(0);
    Thread.sleep(100);

    final WorkflowRun lastBuild = project.getLastBuild();
    JenkinsJobUtils.triggerAbortOnLogLine(lastBuild, "Loading suite");

    WorkflowRun run = runFuture.get();
    Thread.sleep(1000);

    dumpLogs(run);

    checkNoReport(run);
    checkRunAbortedCleanly(run);
    checkApiServerResourcesAreCleaned();
  }

  /**
   * Test that job abort is handled cleanly when Fuzzing just being started or shortly after that.
   */
  @Test
  public void testAbortJob_onFuzzStarting() throws Exception {
    initialSuiteInstanceCount = apiUtils.getSuiteInstances().size();
    ProjectUtils.setupProject(
        jenkinsRule,
        project,
        NAME,
        API_SERVER_URL.toString(),
        CERTIFICATE_VALIDATION_DISABLED,
        credentialsId,
        SETTING_FILE_PATH);

    project.setDefinition(new CpsFlowDefinition(defaultPipelineScript, true));

    // Schedule build
    final QueueTaskFuture<WorkflowRun> runFuture = project.scheduleBuild2(0);
    Thread.sleep(100);

    final WorkflowRun lastBuild = project.getLastBuild();
    JenkinsJobUtils.triggerAbortOnLogLine(lastBuild, "Fuzz testing is starting");

    WorkflowRun run = runFuture.get();
    dumpLogs(run);
    checkRunAndReportPresent(run, Result.ABORTED, true);

    checkRunAbortedCleanly(run);
    checkApiServerResourcesAreCleaned();
  }

  /**
   * Test that job abort is handled cleanly when Fuzz job is RUNNING or shortly after that.
   */
  @Test
  public void testAbortJob_onFuzzing() throws Exception {
    initialSuiteInstanceCount = apiUtils.getSuiteInstances().size();
    ProjectUtils.setupProject(
        jenkinsRule,
        project,
        NAME,
        API_SERVER_URL.toString(),
        CERTIFICATE_VALIDATION_DISABLED,
        credentialsId,
        SETTING_FILE_PATH);

    String pipelineScript = createPipelineScript(
        NAME,
        SETTING_FILE_PATH,
        String.format("--uri %s --index 0-1000", SUT_URI),
        true
    );

    project.setDefinition(new CpsFlowDefinition(pipelineScript, true));

    // Schedule build
    final QueueTaskFuture<WorkflowRun> runFuture = project.scheduleBuild2(0);
    Thread.sleep(100);

    final WorkflowRun lastBuild = project.getLastBuild();
    // Interrupt build after >= 10 test cases have been run and logged like this
    //   [Defensics]  0.6% (  13/1001) of tests run. All passed.
    JenkinsJobUtils.triggerAbortOnLogLine(lastBuild, "[0-9]{2,4}/[0-9]+\\) of tests run");

    WorkflowRun run = runFuture.get();
    dumpLogs(run);

    checkRunAbortedCleanly(run);
    checkRunAndReportPresent(run, Result.ABORTED, true);
    checkResultPackagePresent(run, SETTING_FILE_PATH);
    checkApiServerResourcesAreCleaned();
  }

  /**
   * Test that job abort is handled cleanly when Fuzzing has just been completed.
   */
  @Ignore("Job finishes too fast after run has completed")
  @Test
  public void testAbortJob_onCompletion() throws Exception {
    initialSuiteInstanceCount = apiUtils.getSuiteInstances().size();
    ProjectUtils.setupProject(
        jenkinsRule,
        project,
        NAME,
        API_SERVER_URL.toString(),
        CERTIFICATE_VALIDATION_DISABLED,
        credentialsId,
        SETTING_FILE_PATH);

    project.setDefinition(new CpsFlowDefinition(defaultPipelineScript, true));

    // Schedule build
    final QueueTaskFuture<WorkflowRun> runFuture = project.scheduleBuild2(0);
    Thread.sleep(100);

    final WorkflowRun lastBuild = project.getLastBuild();
    JenkinsJobUtils.triggerAbortOnLogLine(lastBuild, "Fuzz testing is COMPLETED");

    WorkflowRun run = runFuture.get();
    dumpLogs(run);
    checkNoReport(run);

    checkRunAbortedCleanly(run);
    checkApiServerResourcesAreCleaned();
  }

  @Test
  public void testParallelJobStop() throws Exception {
    // Configure concurrent suite runs have more cases so the first suite doesn't stop before last
    // suite has started loading
    final String override = String.format("--uri %s --index 0-5000", SUT_URI);
    String script = String.join("\n", Arrays.asList(
        "node {",
        "  stage('Build') {",
        "     parallel(",
        "      job1: {",
        "         defensics(",
        "           defensicsInstance: '" + NAME + "',",
        "           configurationFilePath: '" + SETTING_FILE_PATH + "',",
        "           configurationOverrides: '" + override + "',",
        "         )",
        "         echo 'Step 2: Should not be shown if interrupted'",
        "      }, job2: {",
        "         defensics(",
        "           defensicsInstance: '" + NAME + "',",
        "           configurationFilePath: '" + SETTING_FILE_PATH + "',",
        "           configurationOverrides: '" + override + "'",
        "         )",
        "         echo 'Step 2: Should not be shown if interrupted'",
        "      }, job3: {",
        "         defensics(",
        "           defensicsInstance: '" + NAME + "',",
        "           configurationFilePath: '" + SETTING_FILE_PATH + "',",
        "           configurationOverrides: '" + override + "'",
        "         )",
        "         echo 'Step 2: Should not be shown if interrupted'",
        "      })",
        "  }",
        "}"
      )
    );
    initialSuiteInstanceCount = apiUtils.getSuiteInstances().size();
    ProjectUtils.setupProject(
        jenkinsRule,
        project,
        NAME,
        API_SERVER_URL.toString(),
        CERTIFICATE_VALIDATION_DISABLED,
        credentialsId,
        SETTING_FILE_PATH);

    project.setDefinition(new CpsFlowDefinition(script, true));

    // Schedule build
    final QueueTaskFuture<WorkflowRun> runFuture = project.scheduleBuild2(0);

    Thread.sleep(500);

    final WorkflowRun lastBuild = project.getLastBuild();

    // All three suite loads need to be started before triggering interrupt.
    JenkinsJobUtils.triggerAbortOnLogLine(lastBuild, "Waiting for.* suite to load", 3);

    WorkflowRun run = runFuture.get();

    final long suiteUnloadCount = run.getLog(999)
        .stream()
        .filter(line -> line.contains("Unloaded suite and deleted the run from API server"))
        .count();

    dumpLogs(run);

    assertThat(suiteUnloadCount, is(3L));
    checkRunAbortedCleanly(run);
    checkApiServerResourcesAreCleaned();
  }

  /**
   * Check that requests failing with TLS configuration mention that.
   */
  @Test
  public void testHealthCheck_reportsTlsProblems() {
    Assume.assumeThat(
        "This test requires that API server is running with HTTPS",
        API_SERVER_URL.getScheme().equals("https"),
        is(true)
    );
    final ApiService apiService = new ApiService(
        API_SERVER_URL.toString(),
        API_TOKEN,
        false
    );

    try {
      apiService.healthCheck();
    } catch (DefensicsRequestException | InterruptedException e) {
      final String expectedErrorMessage =
          "unable to find valid certification path to requested target";
      assertThat(
          e.getMessage(),
          containsString(expectedErrorMessage)
      );
    }
  }

  /**
   * Check that Jenkins logs contain information about 401 unauthorized access if token is wrong.
   */
  @Test
  public void testHealthCheck_reportUnauthorized() {
    Assume.assumeThat(
        "This test requires that API server is running with HTTPS - if HTTP, server could be "
            + "running in insecure mode",
        API_SERVER_URL.getScheme().equals("https"),
        is(true)
    );
    final boolean disableCertValidation = true;
    final ApiService apiService = new ApiService(
        API_SERVER_URL.toString(),
        CredentialsUtil.VALID_TOKEN,
        disableCertValidation
    );

    try {
      apiService.healthCheck();
    } catch (DefensicsRequestException | InterruptedException e) {
      assertThat(e.getMessage(), containsString("401"));
      assertThat(e.getMessage(), containsString("Unauthorized"));
    }
  }

  /**
   * Set up project and make it use given pipeline script
   *
   * @param pipelineScript Script to use
   */
  private void setupProject(String pipelineScript) {
    final CpsFlowDefinition definition = new CpsFlowDefinition(pipelineScript, true);
    project.setDefinition(definition);
    ProjectUtils.setupProject(
        jenkinsRule,
        project,
        NAME,
        API_SERVER_URL.toString(),
        true, credentialsId,
        SETTING_FILE_PATH
    );
  }

  private boolean logHas(WorkflowRun run, String s) throws IOException {
    return Stream.of(run.getLog(999)).anyMatch(line -> line.toString().contains(s));
  }

  private boolean logHas(WorkflowRun run, Pattern regex) throws IOException {
    return Stream.of(run.getLog(999)).anyMatch(line -> regex.matcher(line.toString()).find());
  }

  private void checkRunOkAndReportPresent(WorkflowRun run) throws IOException {
    checkRunAndReportPresent(run, Result.SUCCESS, false);
  }

  private void checkRunAndReportPresent(
      WorkflowRun run,
      Result expectedResult,
      boolean shouldLogPipelineError
  ) throws IOException {
    if (expectedResult.equals(Result.SUCCESS)) {
      assertThat(logHas(run, "100.0%"), is(true));
    }
    assertThat(run.getResult(), is(equalTo(expectedResult)));
    assertThat(run.getActions(HtmlReportAction.class).size(), is(equalTo(1)));
    assertThat(run.getActions(HTMLAction.class).size(), is(equalTo(0)));
    assertThat(project.getActions(HtmlReportAction.class).size(), is(equalTo(1)));
    assertThat(project.getAction(HtmlReportAction.class).getUrlName(),
        is(equalTo(run.getActions(HtmlReportAction.class).get(0).getUrlName())));
    assertThat(run.getLog(100).contains(PIPELINE_ERROR_TEXT), is(shouldLogPipelineError));
  }

  private void checkResultPackagePresent(WorkflowRun run, String testplanFile) {
    assertThat(run.getActions(ResultPackageAction.class).size(), is(1));
    final ResultPackageAction resultPackageAction =
        run.getActions(ResultPackageAction.class).get(0);

    assertThat(resultPackageAction.getResultPackages().size(), is(1));
    final String fileName = resultPackageAction.getResultPackages().get(0);
    assertThat(
        resultPackageAction.getDescription(fileName),
        is(String.format("%s", testplanFile.replace(".testplan", "")))
    );
  }

  private void dumpLogs(WorkflowRun run) throws IOException {
    run.getLog(999).forEach(System.out::println);
  }

  private void checkNoReport(WorkflowRun run) {
    assertThat(run.getActions(HtmlReportAction.class).size(), is(equalTo(0)));
    assertThat(run.getActions(HTMLAction.class).size(), is(equalTo(0)));
    assertThat(project.getActions(HtmlReportAction.class).size(), is(equalTo(0)));
    assertThat(project.getAction(HtmlReportAction.class),
        is(nullValue()));
  }

  private void checkRunAbortedCleanly(WorkflowRun run) throws IOException {
    assertThat(run.getResult(), is(equalTo(Result.ABORTED)));
    assertThat(logHas(run, "Fuzzing was interrupted"), is(true));
    if(logHas(run, "Stopping run")) {
      assertThat(logHas(run, "Stopping succeeded"), is(true));
    }
    assertThat(
        "Second step should not be run after fuzz job is interrupted",
        run.getLog(999).stream().anyMatch(line -> line.contains("Step 2:")),
        is(false)
    );
  }

  private void checkApiServerResourcesAreCleaned() {
    assertThat(
        "Check test: Initial suite count should have been initialized",
        initialSuiteInstanceCount,
        is(not(-1))
    );
    assertThat(
        "There should be no extra suite instances after run has ended",
        apiUtils.getSuiteInstances().size(),
        is(initialSuiteInstanceCount)
    );
  }

  /**
   * Add JSON:API client methods which are not defined in DefensicsJsonApiClient but are needed in
   * these tests.
   */
  public static class ApiUtils {
    private final DefensicsApiClient defensicsApiClient;

    /** Default constructor. */
    public ApiUtils(URI apiBaseUri, String authToken) {
      if (CERTIFICATE_VALIDATION_DISABLED) {
        defensicsApiClient = new DefensicsApiV2Client(
            apiBaseUri,
            authToken,
            UnsafeTlsConfigurator::configureUnsafeTlsOkHttpClient
        );
      } else {
        defensicsApiClient = new DefensicsApiV2Client(
            apiBaseUri,
            authToken
        );
      }
    }

    /**
     * Returns all known suite instances from API server. Used here to test that suites are unloaded
     * after Jenkins job has ended.
     * @return List of suite instances
     */
    public List<SuiteInstance> getSuiteInstances() {
      return defensicsApiClient.getSuiteInstances();
    }
  }
}
