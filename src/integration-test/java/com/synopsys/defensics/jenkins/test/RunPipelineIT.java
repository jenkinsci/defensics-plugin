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

import static com.synopsys.defensics.jenkins.test.utils.Constants.CERTIFICATE_VALIDATION_ENABLED;
import static com.synopsys.defensics.jenkins.test.utils.Constants.LOCAL_URL;
import static com.synopsys.defensics.jenkins.test.utils.Constants.NAME;
import static com.synopsys.defensics.jenkins.test.utils.Constants.PIPELINE_ERROR_TEXT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import com.synopsys.defensics.apiserver.model.RunState;
import com.synopsys.defensics.jenkins.result.HtmlReportPublisherTarget.HtmlReportAction;
import com.synopsys.defensics.jenkins.test.utils.CredentialsUtil;
import com.synopsys.defensics.jenkins.test.utils.DefensicsMockServer;
import com.synopsys.defensics.jenkins.test.utils.JenkinsJobUtils;
import com.synopsys.defensics.jenkins.test.utils.ProjectUtils;
import htmlpublisher.HtmlPublisherTarget.HTMLAction;
import hudson.EnvVars;
import hudson.model.Result;
import hudson.model.queue.QueueTaskFuture;
import hudson.slaves.EnvironmentVariablesNodeProperty;
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
  private static final String PIPELINE_SCRIPT =
      "node { stage('Build') { try { defensics(defensicsInstance:'" + NAME
          + "', configurationFilePath:'" + SETTING_FILE_NAME + "')"
          + "} catch (error) { echo \"" + PIPELINE_ERROR_TEXT + "\"; throw error }}}";
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

    assertThat(run.getResult(), is(equalTo(Result.SUCCESS)));
    assertThat(run.getActions(HtmlReportAction.class).size(), is(equalTo(1)));
    assertThat(run.getActions(HTMLAction.class).size(), is(equalTo(0)));
    assertThat(project.getActions(HtmlReportAction.class).size(), is(equalTo(1)));
    assertThat(project.getAction(HtmlReportAction.class).getUrlName(),
        is(equalTo(run.getActions(HtmlReportAction.class).get(0).getUrlName())));
    assertThat(run.getLog(100).contains(PIPELINE_ERROR_TEXT), is(false));
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

    assertThat(run.getResult(), is(equalTo(Result.ABORTED)));
    assertThat(run.getActions(HtmlReportAction.class).size(), is(equalTo(0)));
    assertThat(run.getActions(HTMLAction.class).size(), is(equalTo(0)));
    assertThat(project.getActions(HtmlReportAction.class).size(), is(equalTo(0)));
    assertThat(project.getAction(HtmlReportAction.class),
        is(nullValue()));
    assertThat(run.getLog(100).contains(PIPELINE_ERROR_TEXT), is(false));
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

    assertThat(run.getResult(), is(equalTo(Result.FAILURE)));
    assertThat(run.getActions(HtmlReportAction.class).size(), is(equalTo(0)));
    assertThat(run.getActions(HTMLAction.class).size(), is(equalTo(0)));
    assertThat(project.getActions(HtmlReportAction.class).size(), is(equalTo(0)));
    assertThat(project.getAction(HtmlReportAction.class),
        is(nullValue()));
    assertThat(run.getLog(100).contains(PIPELINE_ERROR_TEXT), is(true));
  }
}
