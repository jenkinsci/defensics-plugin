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

import static com.synopsys.defensics.jenkins.test.utils.Constants.CERTIFICATE_VALIDATION_ENABLED;
import static com.synopsys.defensics.jenkins.test.utils.Constants.LOCAL_URL;
import static com.synopsys.defensics.jenkins.test.utils.Constants.NAME;
import static com.synopsys.defensics.jenkins.test.utils.Constants.PIPELINE_ERROR_TEXT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import com.synopsys.defensics.apiserver.model.RunState;
import com.synopsys.defensics.jenkins.result.HtmlReportPublisherTarget.HtmlReportAction;
import com.synopsys.defensics.jenkins.result.ResultPackageAction;
import com.synopsys.defensics.jenkins.test.utils.CredentialsUtil;
import com.synopsys.defensics.jenkins.test.utils.DefensicsMockServer;
import com.synopsys.defensics.jenkins.test.utils.JenkinsJobUtils;
import com.synopsys.defensics.jenkins.test.utils.ProjectUtils;
import htmlpublisher.HtmlPublisherTarget.HTMLAction;
import hudson.EnvVars;
import hudson.model.Result;
import hudson.model.queue.QueueTaskFuture;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import java.io.IOException;
import java.util.Arrays;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockserver.integration.ClientAndServer;

public class RunPipelineIT {
  private static final String SETTING_FILE_NAME = "http_1000.set";
  private static final String PIPELINE_SCRIPT = String.join("\n", Arrays.asList(
      "node {",
      "  stage('Build') {",
      "    try {",
      "      defensics(",
      "        defensicsInstance:'" + NAME + "',",
      "        configurationFilePath:'" + SETTING_FILE_NAME + "',",
      "        saveResultPackage: true",
      "      )",
      "    } catch (error) {",
      "      echo \"" + PIPELINE_ERROR_TEXT + "\";",
      "      throw error;",
      "    }",
      "  }",
      "}"
  ));

  // If true, prints Jenkins console logs for each run
  private boolean dumpRunLogs = true;

  private static ClientAndServer mockServer;
  @Rule
  public JenkinsRule jenkinsRule = new JenkinsRule();
  private WorkflowJob project;
  private String credentialsId;

  @Before
  public void setup() throws Exception {
    project = jenkinsRule.createProject(WorkflowJob.class);
    credentialsId = CredentialsUtil.createValidCredentials(jenkinsRule.jenkins);

    EnvironmentVariablesNodeProperty prop = new EnvironmentVariablesNodeProperty();
    EnvVars env = prop.getEnvVars();
    env.put("DEFENSICS_MAX_POLLING_INTERVAL", "1");
    jenkinsRule.jenkins.getGlobalNodeProperties().add(prop);
    mockServer = ClientAndServer.startClientAndServer(1080);
  }

  @After
  public void stopServer() {
    DefensicsMockServer.stopMockServer(mockServer);
  }

  @Test
  public void testRun() throws Exception {
    project.setDefinition(new CpsFlowDefinition(PIPELINE_SCRIPT, true));
    DefensicsMockServer defensicsMockServer = new DefensicsMockServer(true, "PASS", RunState.COMPLETED);
    defensicsMockServer.initServer(RunPipelineIT.mockServer);

    ProjectUtils.setupProject(
        jenkinsRule,
        project,
        NAME,
        LOCAL_URL,
        true, credentialsId,
        SETTING_FILE_NAME);
    WorkflowRun run = project.scheduleBuild2(0).get();
    dumpRunLog(run);

    assertThat(run.getResult(), is(equalTo(Result.SUCCESS)));
    assertThat(run.getActions(HtmlReportAction.class).size(), is(equalTo(1)));
    assertThat(run.getActions(HTMLAction.class).size(), is(equalTo(0)));
    assertThat(project.getActions(HtmlReportAction.class).size(), is(equalTo(1)));
    assertThat(project.getAction(HtmlReportAction.class).getUrlName(),
        is(equalTo(run.getActions(HtmlReportAction.class).get(0).getUrlName())));
    assertThat(run.getLog(100).contains(PIPELINE_ERROR_TEXT), is(false));

    assertThat(run.getActions(ResultPackageAction.class).size(), is(1));
    final ResultPackageAction resultPackageAction =
        run.getActions(ResultPackageAction.class).get(0);

    assertThat(resultPackageAction.getResultPackages().size(), is(1));
    final String fileName = resultPackageAction.getResultPackages().get(0);
    assertThat(
        resultPackageAction.getDescription(fileName),
        is(SETTING_FILE_NAME.replace(".testplan", ""))
    );
  }

  @Test
  public void testRunJobWithFailures() throws Exception {
    project.setDefinition(new CpsFlowDefinition(PIPELINE_SCRIPT, true));
    DefensicsMockServer defensicsMockServer = new DefensicsMockServer(true, "FAIL", RunState.COMPLETED);
    defensicsMockServer.initServer(RunPipelineIT.mockServer);
    ProjectUtils.setupProject(
        jenkinsRule,
        project,
        NAME,
        LOCAL_URL,
        CERTIFICATE_VALIDATION_ENABLED, credentialsId,
        SETTING_FILE_NAME);

    WorkflowRun run = project.scheduleBuild2(0).get();
    dumpRunLog(run);

    assertThat(run.getResult(), is(equalTo(Result.FAILURE)));
    assertThat(run.getActions(HtmlReportAction.class).size(), is(equalTo(1)));
    assertThat(run.getActions(HTMLAction.class).size(), is(equalTo(0)));
    assertThat(project.getActions(HtmlReportAction.class).size(), is(equalTo(1)));
    assertThat(project.getAction(HtmlReportAction.class).getUrlName(),
        is(equalTo(run.getActions(HtmlReportAction.class).get(0).getUrlName())));
    assertThat(run.getLog(100).contains(PIPELINE_ERROR_TEXT), is(true));
    assertThat(run.getLog(100).contains("ERROR: Fuzzing completed with verdict FAIL and 0 "
        + "failures. See Defensics Results for details."), is(true));
  }

  @Test
  public void testAbortJob() throws Exception {
    // Create and use new client to prevent Job from completing.
    DefensicsMockServer defensicsMockServer = new DefensicsMockServer(true, "PASS", RunState.COMPLETED);
    defensicsMockServer.initServer(RunPipelineIT.mockServer);
    ProjectUtils.setupProject(
        jenkinsRule,
        project,
        NAME,
        LOCAL_URL,
        CERTIFICATE_VALIDATION_ENABLED,
        credentialsId,
        SETTING_FILE_NAME);

    project.setDefinition(new CpsFlowDefinition(PIPELINE_SCRIPT, true));

    // Schedule build
    QueueTaskFuture<WorkflowRun> runFuture = project.scheduleBuild2(0);

    // Wait for client.getJob to be called once, then interrupt build.
    Thread.sleep(1000);

    final WorkflowRun lastBuild = project.getLastBuild();
    JenkinsJobUtils.triggerAbortOnLogLine(lastBuild, "Fuzz testing is RUNNING");

    WorkflowRun run = runFuture.get();
    dumpRunLog(run);

    assertThat(run.getResult(), is(equalTo(Result.ABORTED)));
    assertThat(run.getActions(HtmlReportAction.class).size(), is(equalTo(1)));
    assertThat(run.getActions(HTMLAction.class).size(), is(equalTo(0)));
    assertThat(project.getActions(HtmlReportAction.class).size(), is(equalTo(1)));
    assertThat(project.getAction(HtmlReportAction.class), is(notNullValue()));
  }

  @Test
  public void testJobFailed() throws Exception {
    project.setDefinition(new CpsFlowDefinition(PIPELINE_SCRIPT, true));
    DefensicsMockServer mockServer = new DefensicsMockServer(true, "PASS", RunState.ERROR);
    mockServer.initServer(RunPipelineIT.mockServer);
    ProjectUtils.setupProject(
        jenkinsRule,
        project,
        NAME,
        LOCAL_URL,
        CERTIFICATE_VALIDATION_ENABLED,
        credentialsId,
        SETTING_FILE_NAME);

    WorkflowRun run = project.scheduleBuild2(0).get();
    dumpRunLog(run);

    assertThat(run.getResult(), is(equalTo(Result.FAILURE)));
    assertThat(run.getActions(HtmlReportAction.class).size(), is(equalTo(1)));
    assertThat(run.getActions(HTMLAction.class).size(), is(equalTo(0)));
    assertThat(project.getActions(HtmlReportAction.class).size(), is(equalTo(1)));
    assertThat(project.getAction(HtmlReportAction.class), is(notNullValue()));
    assertThat(run.getLog(100).contains(PIPELINE_ERROR_TEXT), is(true));
  }

  @Test
  public void testJobEndedWithoutVerdict() throws Exception {
    project.setDefinition(new CpsFlowDefinition(PIPELINE_SCRIPT, true));
    DefensicsMockServer mockServer = new DefensicsMockServer(true, "NONE", RunState.COMPLETED);
    mockServer.initServer(RunPipelineIT.mockServer);
    ProjectUtils.setupProject(
        jenkinsRule,
        project,
        NAME,
        LOCAL_URL,
        CERTIFICATE_VALIDATION_ENABLED,
        credentialsId,
        SETTING_FILE_NAME);

    WorkflowRun run = project.scheduleBuild2(0).get();
    dumpRunLog(run);

    assertThat(run.getResult(), is(equalTo(Result.FAILURE)));
    assertThat(run.getActions(HtmlReportAction.class).size(), is(equalTo(1)));
    assertThat(run.getActions(HTMLAction.class).size(), is(equalTo(0)));
    assertThat(project.getActions(HtmlReportAction.class).size(), is(equalTo(1)));
    assertThat(project.getAction(HtmlReportAction.class).getUrlName(),
        is(equalTo(run.getActions(HtmlReportAction.class).get(0).getUrlName())));
    assertThat(run.getLog(100).contains(PIPELINE_ERROR_TEXT), is(true));
    String expectedSummary = "ERROR: Fuzzing completed with verdict NONE and 0 failures. "
        + "See Defensics Results for details.";
    assertThat(run.getLog(100).contains(expectedSummary), is(true));
  }

  private void dumpRunLog(WorkflowRun run) throws IOException {
    if (dumpRunLogs) {
      run.getLog(999).forEach(System.out::println);
    }
  }
}
