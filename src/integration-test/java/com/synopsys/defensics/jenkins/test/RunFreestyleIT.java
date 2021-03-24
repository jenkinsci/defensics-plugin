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
import static com.synopsys.defensics.jenkins.test.utils.Constants.SETTING_FILE_PATH;
import static java.lang.Thread.sleep;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import com.synopsys.defensics.apiserver.model.RunState;
import com.synopsys.defensics.jenkins.result.HtmlReportPublisherTarget.HtmlReportAction;
import com.synopsys.defensics.jenkins.test.utils.CredentialsUtil;
import com.synopsys.defensics.jenkins.test.utils.DefensicsMockServer;
import com.synopsys.defensics.jenkins.test.utils.ProjectUtils;
import htmlpublisher.HtmlPublisherTarget.HTMLAction;
import hudson.EnvVars;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.queue.QueueTaskFuture;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockserver.integration.ClientAndServer;

public class RunFreestyleIT {

  @Rule
  public JenkinsRule jenkinsRule = new JenkinsRule();

  private FreeStyleProject project;
  private ClientAndServer mockServer;
  private String credentialsId;

  @Before
  public void setup() throws Exception {
    project = jenkinsRule.createFreeStyleProject();
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
  public void testRunBuildStep() throws Exception {
    DefensicsMockServer defensicsMockServer = new DefensicsMockServer(true, "PASS", RunState.COMPLETED);
    defensicsMockServer.initServer(this.mockServer);
    ProjectUtils.setupProject(
        jenkinsRule,
        project,
        NAME,
        LOCAL_URL,
        CERTIFICATE_VALIDATION_ENABLED,
        credentialsId,
        SETTING_FILE_PATH);
    ProjectUtils.addBuildStep(project, NAME, SETTING_FILE_PATH, false);

    FreeStyleBuild run = project.scheduleBuild2(0).get();

    assertThat(run.getResult(), is(equalTo(Result.SUCCESS)));
    assertThat(run.getActions(HtmlReportAction.class).size(), is(equalTo(1)));
    assertThat(run.getActions(HTMLAction.class).size(), is(equalTo(0)));
    assertThat(project.getAction(HtmlReportAction.class).getUrlName(),
        is(equalTo(run.getActions(HtmlReportAction.class).get(0).getUrlName())));
  }

  @Test
  public void testRunPostBuildStep() throws Exception {
    DefensicsMockServer defensicsMockServer = new DefensicsMockServer(true, "PASS", RunState.COMPLETED);
    defensicsMockServer.initServer(this.mockServer);
    ProjectUtils.setupProject(
        jenkinsRule,
        project,
        NAME,
        LOCAL_URL,
        CERTIFICATE_VALIDATION_ENABLED,
        credentialsId,
        SETTING_FILE_PATH);
    ProjectUtils.addPostBuildStep(project, NAME, SETTING_FILE_PATH, false);

    FreeStyleBuild run = project.scheduleBuild2(0).get();

    assertThat(run.getResult(), is(equalTo(Result.SUCCESS)));
    assertThat(run.getActions(HtmlReportAction.class).size(), is(equalTo(1)));
    assertThat(run.getActions(HTMLAction.class).size(), is(equalTo(0)));
    assertThat(project.getAction(HtmlReportAction.class).getUrlName(),
        is(equalTo(run.getActions(HtmlReportAction.class).get(0).getUrlName())));
  }

  @Test
  public void testConfigurationRoundTripAndRun() throws Exception {
    DefensicsMockServer defensicsMockServer = new DefensicsMockServer(true, "PASS", RunState.COMPLETED);
    defensicsMockServer.initServer(this.mockServer);
    ProjectUtils.setupProject(
        jenkinsRule,
        project,
        NAME,
        LOCAL_URL,
        CERTIFICATE_VALIDATION_ENABLED,
        credentialsId,
        SETTING_FILE_PATH);
    ProjectUtils.addBuildStep(project, NAME, SETTING_FILE_PATH, false);

    jenkinsRule.configRoundtrip(project);

    FreeStyleBuild run = project.scheduleBuild2(0).get();

    assertThat(run.getResult(), is(equalTo(Result.SUCCESS)));
    assertThat(run.getActions(HtmlReportAction.class).size(), is(equalTo(1)));
    assertThat(run.getActions(HTMLAction.class).size(), is(equalTo(0)));
    assertThat(project.getAction(HtmlReportAction.class).getUrlName(),
        is(equalTo(run.getActions(HtmlReportAction.class).get(0).getUrlName())));
  }

  @Test
  public void testAbortJob() throws Exception {
    DefensicsMockServer defensicsMockServer = new DefensicsMockServer(true, "PASS", RunState.COMPLETED);
    defensicsMockServer.initServer(this.mockServer);
    ProjectUtils.setupProject(
        jenkinsRule,
        project,
        NAME,
        LOCAL_URL,
        CERTIFICATE_VALIDATION_ENABLED,
        credentialsId,
        SETTING_FILE_PATH);
    ProjectUtils.addBuildStep(project, NAME, SETTING_FILE_PATH, false);

    // Schedule build
    QueueTaskFuture<FreeStyleBuild> runFuture = project.scheduleBuild2(0);

    // Wait for job to start properly, then interrupt build.
    sleep(2000);
    runFuture.cancel(true);

    FreeStyleBuild run = runFuture.get();

    assertThat(run.getResult(), is(equalTo(Result.ABORTED)));
    assertThat(run.getActions(HtmlReportAction.class).size(), is(equalTo(0)));
    assertThat(run.getActions(HTMLAction.class).size(), is(equalTo(0)));
    assertThat(project.getAction(HtmlReportAction.class),
        is(nullValue()));
  }

  @Test
  public void testJobFailed() throws Exception {
    DefensicsMockServer defensicsMockServer = new DefensicsMockServer(true, "PASS", RunState.ERROR);
    defensicsMockServer.initServer(this.mockServer);
    ProjectUtils.setupProject(
        jenkinsRule,
        project,
        NAME,
        LOCAL_URL,
        CERTIFICATE_VALIDATION_ENABLED,
        credentialsId,
        SETTING_FILE_PATH);
    ProjectUtils.addBuildStep(project, NAME, SETTING_FILE_PATH, false);

    FreeStyleBuild run = project.scheduleBuild2(0).get();

    assertThat(run.getResult(), is(equalTo(Result.FAILURE)));
    assertThat(run.getActions(HtmlReportAction.class).size(), is(equalTo(0)));
    assertThat(run.getActions(HTMLAction.class).size(), is(equalTo(0)));
    assertThat(project.getAction(HtmlReportAction.class),
        is(nullValue()));
  }

  @Test
  public void testJobFailure() throws Exception {
    DefensicsMockServer defensicsMockServer = new DefensicsMockServer(true, "FAIL", RunState.COMPLETED);
    defensicsMockServer.initServer(this.mockServer);

    ProjectUtils.setupProject(
        jenkinsRule,
        project,
        NAME,
        LOCAL_URL,
        CERTIFICATE_VALIDATION_ENABLED,
        credentialsId,
        SETTING_FILE_PATH);
    ProjectUtils.addBuildStep(project, NAME, SETTING_FILE_PATH, false);

    FreeStyleBuild run = project.scheduleBuild2(0).get();

    assertThat(run.getResult(), is(equalTo(Result.FAILURE)));
    assertThat(run.getActions(HtmlReportAction.class).size(), is(equalTo(1)));
    assertThat(run.getActions(HTMLAction.class).size(), is(equalTo(0)));
    assertThat(project.getAction(HtmlReportAction.class).getUrlName(),
        is(equalTo(run.getActions(HtmlReportAction.class).get(0).getUrlName())));
  }
}
