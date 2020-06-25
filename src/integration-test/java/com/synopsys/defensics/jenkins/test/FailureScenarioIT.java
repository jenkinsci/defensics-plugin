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

package com.synopsys.defensics.jenkins.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import com.synopsys.defensics.api.ApiService;
import com.synopsys.defensics.apiserver.client.DefensicsJsonApiClient;
import com.synopsys.defensics.apiserver.model.SuiteInstance;
import com.synopsys.defensics.client.DefensicsRequestException;
import com.synopsys.defensics.client.UnsafeTlsConfigurator;
import com.synopsys.defensics.jenkins.result.HtmlReportPublisherTarget.HtmlReportAction;
import com.synopsys.defensics.jenkins.test.utils.CredentialsUtil;
import com.synopsys.defensics.jenkins.test.utils.ProjectUtils;
import htmlpublisher.HtmlPublisherTarget.HTMLAction;
import hudson.EnvVars;
import hudson.model.Result;
import hudson.model.queue.QueueTaskFuture;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import io.crnk.client.CrnkClient;
import io.crnk.core.queryspec.QuerySpec;
import io.crnk.core.repository.ResourceRepository;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Assume;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * End-to-end tests testing failure modes: cases where job fails in different stage in various
 * ways. This could be moved outside of integration tests, e.g. e2e-tests when final run environment
 * has been decided. Now this is run manually by setting values in the beginning of this class.
 *
 * <p>NOTE about interrupted jobs: Currently tests trigger job stop when logs contain given
 * keywords. This makes the exact spot where stop happens in job in-precise and could cause some
 * jitter in tests.
 * </p>
 *
 * <p>To be able to run these tests, check that things listed in external dependencies are
 * enabled and their configuration is correct.
 * </p>
 *
 * <p>Other things to test:
 * <ul>
 *   <li>Jenkins has HTTP address, but server is using HTTPS</li>
 *   <li>Test that API server reports that suite from testplan is not found.</li>
 *   <li>Test that error is reported if client sends a request body which server doesn't
 *   recognize. This comes in case Jenkins plugin models are out-of-sync with server models</li>
 *   <li>etc...</li>
 * </ul>
 */
public class FailureScenarioIT {

  /** Tests are not run until this is true. */
  private static final boolean hasRequiredDependencies = false;

  /*
   * Required external dependencies
   * o Defensics API Server running.
   *   Should have HTTP Server suite 4.11.1 installed and license for it.
   * o HTTP SUT. Now 'python -m SimpleHTTPServer 7000' has been used.
   */
  /** API Server address. */
  private static final String API_SERVER_URL = "http://127.0.0.1:3150";

  /**
   * Set to true if API server has been started with --enable-script-execution.
   * Used to determine if error reporting tests can be run.
   */
  private static final boolean API_SERVER_HAS_ENABLED_EXTERNAL_INSTRUMENTATION = false;

  /** Used SUT address. */
  private static final String SUT_URI = "http://127.0.0.1:7000";

  private static final String NAME = "My Defensics";
  private static final boolean CERTIFICATE_VALIDATION_DISABLED = true;
  private static final String SETTING_FILE_NAME = "http.testplan";
  private static final String PIPELINE_ERROR_TEXT = "Pipeline found error";
  private String pipelineScript = createPipelineScript(
      NAME,
      SETTING_FILE_NAME,
      String.format("--uri %s", SUT_URI)
  );

  private String credentialsId;
  private int initialSuiteInstanceCount = -1;

  @Rule
  public JenkinsRule jenkinsRule = new JenkinsRule();
  private WorkflowJob project;
  private ApiUtils apiUtils;

  @ClassRule
  public static TemporaryFolder temporaryFolder = new TemporaryFolder();

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
      String configurationOverride
  ) {
    return "node {\n"
        + "  stage('Build') {\n"
        + "    try {\n"
        + "     defensics(\n"
        + "       defensicsInstance: '" + defensicsInstance + "', "
        + "       configurationFilePath: '" + configurationFilePath + "', "
        + "       configurationOverrides: '" + configurationOverride + "'"
        + "     )\n"
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

    credentialsId = CredentialsUtil.createValidCredentials(jenkinsRule.jenkins);
    project = jenkinsRule.createProject(WorkflowJob.class);

    apiUtils = new ApiUtils(
        URI.create(API_SERVER_URL).resolve("/api/v1"),
        CredentialsUtil.VALID_TOKEN);
  }

  /**
   * Test that suite is unloaded after test run.
   */
  @Test
  public void testRun_suiteShouldBeUnloaded() throws Exception {
    initialSuiteInstanceCount = apiUtils.getSuiteInstances().size();
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
    pipelineScript = createPipelineScript(
        NAME,
        SETTING_FILE_NAME,
        String.format("--uri %s", wrongSutUri)
    );
    initialSuiteInstanceCount = apiUtils.getSuiteInstances().size();
    setupProject(pipelineScript);

    WorkflowRun run = project.scheduleBuild2(0).get();
    dumpLogs(run);

    String expectedMessage = "Suite error: Unknown host 'non-routable.invalid'";
    assertThat(logHas(run, expectedMessage), is(true));
    assertThat(run.getResult(), is(equalTo(Result.FAILURE)));

    checkNoReport(run);
    checkApiServerResourcesAreCleaned();
  }

  /**
   * Test that Jenkins runs report at least WARNING if SUT port was wrong. NOTE: Unreliable test
   * if host has a service using this port.
   */
  @Test
  public void testRun_wrongPort() throws Exception {
    final String wrongSutUri = "http://127.0.0.1:9999";
    pipelineScript = createPipelineScript(
        NAME,
        SETTING_FILE_NAME,
        String.format("--uri %s", wrongSutUri)
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
    checkNoReport(run);
    checkApiServerResourcesAreCleaned();
  }

  @Test
  public void testRun_emptyTestplan() throws Exception {
    final File emptyFile = temporaryFolder.newFile();

    final String pipelineScriptEmptyTestplan = createPipelineScript(
        NAME,
        emptyFile.getAbsolutePath(),
        ""
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
        SETTING_FILE_NAME,
        String.format("--exec-instrument echo 1", SUT_URI)
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
        API_SERVER_URL,
        CERTIFICATE_VALIDATION_DISABLED,
        credentialsId,
        SETTING_FILE_NAME);

    project.setDefinition(new CpsFlowDefinition(pipelineScript, true));

    // Schedule build
    final QueueTaskFuture<WorkflowRun> runFuture = project.scheduleBuild2(0);

    Thread.sleep(100);

    final WorkflowRun lastBuild = project.getLastBuild();
    triggerAbortOnLogLine(runFuture, lastBuild, "Creating");

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
        API_SERVER_URL,
        CERTIFICATE_VALIDATION_DISABLED,
        credentialsId,
        SETTING_FILE_NAME);

    project.setDefinition(new CpsFlowDefinition(pipelineScript, true));

    // Schedule build
    final QueueTaskFuture<WorkflowRun> runFuture = project.scheduleBuild2(0);
    Thread.sleep(100);

    final WorkflowRun lastBuild = project.getLastBuild();
    triggerAbortOnLogLine(runFuture, lastBuild, "Loading suite");

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
        API_SERVER_URL,
        CERTIFICATE_VALIDATION_DISABLED,
        credentialsId,
        SETTING_FILE_NAME);

    project.setDefinition(new CpsFlowDefinition(pipelineScript, true));

    // Schedule build
    final QueueTaskFuture<WorkflowRun> runFuture = project.scheduleBuild2(0);
    Thread.sleep(100);

    final WorkflowRun lastBuild = project.getLastBuild();
    triggerAbortOnLogLine(runFuture, lastBuild, "Fuzz testing is starting");

    WorkflowRun run = runFuture.get();
    dumpLogs(run);
    checkNoReport(run);

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
        API_SERVER_URL,
        CERTIFICATE_VALIDATION_DISABLED,
        credentialsId,
        SETTING_FILE_NAME);

    project.setDefinition(new CpsFlowDefinition(pipelineScript, true));

    // Schedule build
    final QueueTaskFuture<WorkflowRun> runFuture = project.scheduleBuild2(0);
    Thread.sleep(100);

    final WorkflowRun lastBuild = project.getLastBuild();
    triggerAbortOnLogLine(runFuture, lastBuild, "Fuzz testing is RUNNING.");

    WorkflowRun run = runFuture.get();
    dumpLogs(run);
    checkNoReport(run);

    checkRunAbortedCleanly(run);
    checkApiServerResourcesAreCleaned();
  }

  /**
   * Test that job abort is handled cleanly when Fuzzing has just been completed.
   */
  @Test
  public void testAbortJob_onCompletion() throws Exception {
    initialSuiteInstanceCount = apiUtils.getSuiteInstances().size();
    ProjectUtils.setupProject(
        jenkinsRule,
        project,
        NAME,
        API_SERVER_URL,
        CERTIFICATE_VALIDATION_DISABLED,
        credentialsId,
        SETTING_FILE_NAME);

    project.setDefinition(new CpsFlowDefinition(pipelineScript, true));

    // Schedule build
    final QueueTaskFuture<WorkflowRun> runFuture = project.scheduleBuild2(0);
    Thread.sleep(100);

    final WorkflowRun lastBuild = project.getLastBuild();
    triggerAbortOnLogLine(runFuture, lastBuild, "Fuzz testing is COMPLETED");

    WorkflowRun run = runFuture.get();
    dumpLogs(run);
    checkNoReport(run);

    checkRunAbortedCleanly(run);
    checkApiServerResourcesAreCleaned();
  }

  @Test
  public void testParallelJobStop() throws Exception {
    final String override = String.format("--uri %s", SUT_URI);
    String script = String.join("\n", Arrays.asList(
        "node {",
        "  stage('Build') {",
        "     parallel(",
        "      job1: {",
        "         defensics(",
        "           defensicsInstance: '" + NAME + "',",
        "           configurationFilePath: '" + SETTING_FILE_NAME + "',",
        "           configurationOverrides: '" + override + "',",
        "         )",
        "      }, job2: {",
        "         defensics(",
        "           defensicsInstance: '" + NAME + "',",
        "           configurationFilePath: '" + SETTING_FILE_NAME + "',",
        "           configurationOverrides: '" + override + "'",
        "         )",
        "      }, job3: {",
        "         defensics(",
        "           defensicsInstance: '" + NAME + "',",
        "           configurationFilePath: '" + SETTING_FILE_NAME + "',",
        "           configurationOverrides: '" + override + "'",
        "         )",
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
        API_SERVER_URL,
        CERTIFICATE_VALIDATION_DISABLED,
        credentialsId,
        SETTING_FILE_NAME);

    project.setDefinition(new CpsFlowDefinition(script, true));

    // Schedule build
    final QueueTaskFuture<WorkflowRun> runFuture = project.scheduleBuild2(0);
    Thread.sleep(100);

    final WorkflowRun lastBuild = project.getLastBuild();
    triggerAbortOnLogLine(runFuture, lastBuild, "Loading suite");

    WorkflowRun run = runFuture.get();

    final long suiteUnloadCount = run.getLog(999)
        .stream()
        .filter(line -> line.contains("Unloaded suite and deleted the run from API server"))
        .count();

    assertThat(suiteUnloadCount, is(3L));
    checkRunAbortedCleanly(run);
    checkApiServerResourcesAreCleaned();
  }

  /**
   * Check that requests failing with TLS configuration mention that.
   */
  @Test
  public void testHealthcheck_reportsTlsProblems() {
    Assume.assumeThat(
        "This test requires that API server is running with HTTPS",
        API_SERVER_URL.startsWith("https://"),
        is(true)
    );
    final ApiService apiService = new ApiService(
        API_SERVER_URL,
        CredentialsUtil.VALID_TOKEN,
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
  public void testHealthcheck_reportUnauthorized() {
    Assume.assumeThat(
        "This test requires that API server is running with HTTPS - if HTTP, server could be "
            + "running in insecure mode",
        API_SERVER_URL.startsWith("https://"),
        is(true)
    );
    final boolean disableCertValidation = true;
    final ApiService apiService = new ApiService(
        API_SERVER_URL,
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
   * @param pipelineScript Script to use
   * @throws Exception
   */
  private void setupProject(String pipelineScript) throws Exception {
    final CpsFlowDefinition definition = new CpsFlowDefinition(pipelineScript, true);
    project.setDefinition(definition);
    ProjectUtils.setupProject(
        jenkinsRule,
        project,
        NAME,
        API_SERVER_URL,
        true, credentialsId,
        SETTING_FILE_NAME
    );
  }

  private boolean logHas(WorkflowRun run, String s) throws IOException {
    return Stream.of(run.getLog(999)).anyMatch(line -> line.toString().contains(s));
  }

  private void checkRunOkAndReportPresent(WorkflowRun run) throws IOException {
    assertThat(logHas(run, "100.0%"), is(true));
    assertThat(run.getResult(), is(equalTo(Result.SUCCESS)));
    assertThat(run.getActions(HtmlReportAction.class).size(), is(equalTo(1)));
    assertThat(run.getActions(HTMLAction.class).size(), is(equalTo(0)));
    assertThat(project.getActions(HtmlReportAction.class).size(), is(equalTo(1)));
    assertThat(project.getAction(HtmlReportAction.class).getUrlName(),
        is(equalTo(run.getActions(HtmlReportAction.class).get(0).getUrlName())));
    assertThat(run.getLog(100).contains(PIPELINE_ERROR_TEXT), is(false));
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
    assertThat(run.getLog(100).contains(PIPELINE_ERROR_TEXT), is(false));
    assertThat(logHas(run, "Fuzzing was interrupted"), is(true));
    if(logHas(run, "Stopping run")) {
      assertThat(logHas(run, "Stopping succeeded"), is(true));
    }
  }

  private void triggerAbortOnLogLine(
      QueueTaskFuture<WorkflowRun> runFuture,
      WorkflowRun lastBuild,
      String logString
  ) {
    Executors.newSingleThreadExecutor().submit(() -> {
          try {
            while (true) {
              final boolean hasLogString = Stream
                  .of(lastBuild.getLog(100))
                  .anyMatch(line -> line.toString().contains(logString));

              if (hasLogString) {

                dumpLogs(lastBuild);
                System.out.println("===");
                System.out.println("Found line, aborting");
                // Use Jenkins' own Stop request instead of cancelling future
                lastBuild.doStop();
                //runFuture.cancel(true);
                return;
              }
              // Busy loop, replace this triggering with better one if found. Two problems:
              // 1) Doesn't allow precise abort on given step
              // 2) Extraneous log polling
              Thread.sleep(50);
            }
          } catch (InterruptedException | IOException e) {
            e.printStackTrace();
          }
        }
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
    private final DefensicsJsonApiClient defensicsJsonApiClient;

    /** Default constructor. */
    public ApiUtils(URI apiBaseUri, String authToken) {
      if (CERTIFICATE_VALIDATION_DISABLED) {
        defensicsJsonApiClient = new DefensicsJsonApiClient(
            apiBaseUri,
            authToken,
            UnsafeTlsConfigurator::configureUnsafeTlsOkHttpClient
        );
      } else {
        defensicsJsonApiClient = new DefensicsJsonApiClient(
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
      final CrnkClient crnkClient = defensicsJsonApiClient.getCrnkClient();
      final ResourceRepository<SuiteInstance, String> suiteInstanceRepository = crnkClient
          .getRepositoryForType(SuiteInstance.class);
      return new ArrayList<>(suiteInstanceRepository.findAll(new QuerySpec(SuiteInstance.class)));
    }
  }
}
