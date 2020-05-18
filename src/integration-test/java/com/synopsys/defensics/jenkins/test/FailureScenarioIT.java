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

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.synopsys.defensics.api.ApiService;
import com.synopsys.defensics.apiserver.client.DefensicsJsonApiClient;
import com.synopsys.defensics.apiserver.model.SuiteInstance;
import com.synopsys.defensics.client.DefensicsRequestException;
import com.synopsys.defensics.client.UnsafeTlsConfigurator;
import com.synopsys.defensics.jenkins.result.HtmlReportPublisherTarget.HtmlReportAction;
import com.synopsys.defensics.jenkins.test.utils.ProjectUtils;
import htmlpublisher.HtmlPublisherTarget.HTMLAction;
import hudson.EnvVars;
import hudson.model.Result;
import hudson.model.queue.QueueTaskFuture;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.util.Secret;
import io.crnk.client.CrnkClient;
import io.crnk.core.queryspec.QuerySpec;
import io.crnk.core.repository.ResourceRepository;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * End-to-end tests testing failure modes: cases where job fails in different stage in various
 * ways. This could be moved outside of integration tests, e.g. e2e-tests when final run environment
 * has been decided. Now this is run manually by settings values in the beginning of this class.
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
 *   <li> Jenkins has HTTP address, but server is using HTTPS</li>
 *   <li>Test that API server reports that suite from testplan is not found.</li>
 *   <li>Test that error is reported if client sends a request body which server doesn't
 *   recognize. This comes in case Jenkins plugin models are out-of-sync with server models</li>
 *   <li>o etc...</li>
 * </ul>
 */
public class FailureScenarioIT {

  /** Tests are not run until this is true. */
  private static final boolean hasRequiredDependencies = false;

  /*
   * Required external dependencies
   * o Defensics API Server running.
   *   Should have HTTP Server suite 4.11 installed and license for it.
   * o HTTP SUT. Now 'python -m SimpleHTTPServer 7000' has been used.
   */
  /** API Server address. */
  private static final String API_SERVER_URL = "http://127.0.0.1:3150";

  /** API Server authentication token. */
  private static final String AUTH_TOKEN = "test-token";

  /** Used SUT address. */
  private static final String SUT_URI = "http://127.0.0.1:7000";

  private static final String NAME = "My Defensics";
  private static final boolean CERTIFICATE_VALIDATION_DISABLED = true;
  private static final String CREDENTIAL_ID = "test-credential";
  private static final String SETTING_FILE_NAME = "http.testplan";
  private static final String PIPELINE_ERROR_TEXT = "Pipeline found error";
  private String pipelineScript = createPipelineScript(
      NAME,
      SETTING_FILE_NAME,
      String.format("--uri %s", SUT_URI)
  );

  private int initialSuiteInstanceCount = -1;

  @Rule
  public JenkinsRule jenkinsRule = new JenkinsRule();
  private WorkflowJob project;
  private ApiUtils apiUtils;

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
        + "      echo error.getMessage();"
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

    project = jenkinsRule.createProject(WorkflowJob.class);
    CredentialsStore store = CredentialsProvider.lookupStores(jenkinsRule.jenkins)
        .iterator()
        .next();
    StringCredentialsImpl credential = new StringCredentialsImpl(
        CredentialsScope.GLOBAL,
        CREDENTIAL_ID,
        "Test Secret Text",
        Secret.fromString(AUTH_TOKEN));
    store.addCredentials(Domain.global(), credential);

    apiUtils = new ApiUtils(URI.create(API_SERVER_URL).resolve("/api/v1"), AUTH_TOKEN);
  }

  /**
   * DEF-11341: Test that suite is unloaded after test run.
   */
  @Test
  public void testRun_suiteShouldBeUnloaded() throws Exception {
    initialSuiteInstanceCount = apiUtils.getSuiteInstances().size();
    final CpsFlowDefinition definition = new CpsFlowDefinition(pipelineScript, true);
    project.setDefinition(definition);
    CredentialsStore store = CredentialsProvider.lookupStores(jenkinsRule.jenkins)
        .iterator()
        .next();
    StringCredentialsImpl credential = new StringCredentialsImpl(
        CredentialsScope.GLOBAL,
        CREDENTIAL_ID,
        "Test Secret Text",
        Secret.fromString(AUTH_TOKEN));
    store.addCredentials(Domain.global(), credential);
    ProjectUtils.setupProject(
        jenkinsRule,
        project,
        NAME,
        API_SERVER_URL,
        true, CREDENTIAL_ID,
        SETTING_FILE_NAME);

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
    final CpsFlowDefinition definition = new CpsFlowDefinition(pipelineScript, true);
    project.setDefinition(definition);
    CredentialsStore store = CredentialsProvider.lookupStores(jenkinsRule.jenkins)
        .iterator()
        .next();
    StringCredentialsImpl credential = new StringCredentialsImpl(
        CredentialsScope.GLOBAL,
        CREDENTIAL_ID,
        "Test Secret Text",
        Secret.fromString(AUTH_TOKEN));
    store.addCredentials(Domain.global(), credential);
    ProjectUtils.setupProject(
        jenkinsRule,
        project,
        NAME,
        API_SERVER_URL,
        true, CREDENTIAL_ID,
        SETTING_FILE_NAME);

    WorkflowRun run = project.scheduleBuild2(0).get();
    dumpLogs(run);

    // Ideally API server should say that connection was not working
    assertThat(logHas(run, "Verdict: FAIL"), is(true));
    assertThat(run.getResult(), is(equalTo(Result.FAILURE)));
    assertThat(run.getActions(HtmlReportAction.class).size(), is(equalTo(1)));
    assertThat(run.getActions(HTMLAction.class).size(), is(equalTo(0)));
    assertThat(project.getActions(HtmlReportAction.class).size(), is(equalTo(1)));
    assertThat(project.getAction(HtmlReportAction.class).getUrlName(),
        is(equalTo(run.getActions(HtmlReportAction.class).get(0).getUrlName())));
    assertThat(run.getLog(100).contains(PIPELINE_ERROR_TEXT), is(false));
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
    final CpsFlowDefinition definition = new CpsFlowDefinition(pipelineScript, true);
    project.setDefinition(definition);
    CredentialsStore store = CredentialsProvider.lookupStores(jenkinsRule.jenkins)
        .iterator()
        .next();
    StringCredentialsImpl credential = new StringCredentialsImpl(
        CredentialsScope.GLOBAL,
        CREDENTIAL_ID,
        "Test Secret Text",
        Secret.fromString(AUTH_TOKEN));
    store.addCredentials(Domain.global(), credential);
    ProjectUtils.setupProject(
        jenkinsRule,
        project,
        NAME,
        API_SERVER_URL,
        true, CREDENTIAL_ID,
        SETTING_FILE_NAME);

    WorkflowRun run = project.scheduleBuild2(0).get();
    dumpLogs(run);

    // Ideally API server should say that connection was not working
    assertThat(
        logHas(run, "Verdict: WARNING"),
        is(true)
    );
    assertThat(run.getResult(), is(equalTo(Result.FAILURE)));
    assertThat(run.getActions(HtmlReportAction.class).size(), is(equalTo(1)));
    assertThat(run.getActions(HTMLAction.class).size(), is(equalTo(0)));
    assertThat(project.getActions(HtmlReportAction.class).size(), is(equalTo(1)));
    assertThat(project.getAction(HtmlReportAction.class).getUrlName(),
        is(equalTo(run.getActions(HtmlReportAction.class).get(0).getUrlName())));
    assertThat(run.getLog(100).contains(PIPELINE_ERROR_TEXT), is(false));
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
    // Create and use new client to prevent Job from completing.
    ProjectUtils.setupProject(
        jenkinsRule,
        project,
        NAME,
        API_SERVER_URL,
        CERTIFICATE_VALIDATION_DISABLED,
        CREDENTIAL_ID,
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
   * Test that job abort is handled cleanly when Fuzzing just being started or shortly after that.
   */
  @Test
  public void testAbortJob_onFuzzStarting() throws Exception {
    initialSuiteInstanceCount = apiUtils.getSuiteInstances().size();
    // Create and use new client to prevent Job from completing.
    ProjectUtils.setupProject(
        jenkinsRule,
        project,
        NAME,
        API_SERVER_URL,
        CERTIFICATE_VALIDATION_DISABLED,
        CREDENTIAL_ID,
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
    // Create and use new client to prevent Job from completing.
    ProjectUtils.setupProject(
        jenkinsRule,
        project,
        NAME,
        API_SERVER_URL,
        CERTIFICATE_VALIDATION_DISABLED,
        CREDENTIAL_ID,
        SETTING_FILE_NAME);

    project.setDefinition(new CpsFlowDefinition(pipelineScript, true));

    // Schedule build
    final QueueTaskFuture<WorkflowRun> runFuture = project.scheduleBuild2(0);
    Thread.sleep(100);

    final WorkflowRun lastBuild = project.getLastBuild();
    // Trigger abort on first status line
    // [Defensics] 45.5% ( 46/101) of tests run. All passed.
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
    // Create and use new client to prevent Job from completing.
    ProjectUtils.setupProject(
        jenkinsRule,
        project,
        NAME,
        API_SERVER_URL,
        CERTIFICATE_VALIDATION_DISABLED,
        CREDENTIAL_ID,
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
        AUTH_TOKEN,
        false
    );

    try {
      apiService.healthCheck();
    } catch (DefensicsRequestException e) {
      final String expectedErrorMessage =
          "unable to find valid certification path to requested target";
      assertThat(
          e.getMessage(),
          containsString(expectedErrorMessage)
      );
    }
  }

  private boolean logHas(WorkflowRun run, String s) throws IOException {
    return Stream.of(run.getLog(999)).anyMatch(line -> line.toString().contains(s));
  }

  private void checkRunOkAndReportPresent(WorkflowRun run) throws IOException {
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
