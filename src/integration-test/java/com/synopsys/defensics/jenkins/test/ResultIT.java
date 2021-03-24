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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.synopsys.defensics.apiserver.model.RunState;
import com.synopsys.defensics.jenkins.result.HtmlReportPublisherTarget.HtmlReportAction;
import com.synopsys.defensics.jenkins.result.ResultPublisher;
import com.synopsys.defensics.jenkins.result.history.ProjectHistoryAction;
import com.synopsys.defensics.jenkins.test.utils.CredentialsUtil;
import com.synopsys.defensics.jenkins.test.utils.DefensicsMockServer;
import com.synopsys.defensics.jenkins.test.utils.ProjectUtils;
import hudson.EnvVars;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockserver.integration.ClientAndServer;

public class ResultIT {

  private static ClientAndServer mockServer;
  @Rule
  public JenkinsRule jenkinsRule = new JenkinsRule();
  private FreeStyleProject project;
  private String credentialsId;

  @Before
  public void setup() throws Exception {
    credentialsId = CredentialsUtil.createValidCredentials(jenkinsRule.jenkins);
    project = jenkinsRule.createFreeStyleProject();
    ProjectUtils.setupProject(
        jenkinsRule,
        project,
        NAME,
        LOCAL_URL,
        CERTIFICATE_VALIDATION_ENABLED,
        credentialsId,
        SETTING_FILE_PATH);
    ProjectUtils.addBuildStep(project, NAME, SETTING_FILE_PATH, false);

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
  public void testPublishReport() throws Exception {
    DefensicsMockServer defensicsMockServer = new DefensicsMockServer(false, "PASS", RunState.COMPLETED);
    defensicsMockServer.initServer(ResultIT.mockServer);

    try (JenkinsRule.WebClient webClient = jenkinsRule.createWebClient()) {
      List<String> expectedTabNames = Collections.singletonList(SETTING_FILE_PATH);

      FreeStyleBuild run = project.scheduleBuild2(0).get();

      assertThat(run.getActions(HtmlReportAction.class).size(), is(equalTo(1)));
      doAssertionsOnReport(
          webClient.getPage(project),
          expectedTabNames);
      doAssertionsOnReport(
          webClient.getPage(run),
          expectedTabNames);
    }
  }

  @Test
  public void testPublishReportWithFailures() throws Exception {
    DefensicsMockServer mockServer = new DefensicsMockServer(false, "FAIL", RunState.COMPLETED);
    mockServer.initServer(ResultIT.mockServer);

    try (JenkinsRule.WebClient webClient = jenkinsRule.createWebClient()) {
      List<String> expectedTabNames = Collections.singletonList(SETTING_FILE_PATH);

      FreeStyleBuild run = project.scheduleBuild2(0).get();

      assertThat(run.getActions(HtmlReportAction.class).size(), is(equalTo(1)));
      doAssertionsOnReport(
          webClient.getPage(project),
          expectedTabNames);
      doAssertionsOnReport(
          webClient.getPage(run),
          expectedTabNames);
    }
  }

  @Test
  public void testPublishMultipleReports() throws Exception {
    DefensicsMockServer mockServer = new DefensicsMockServer(false, "PASS", RunState.COMPLETED);
    mockServer.initServer(ResultIT.mockServer);

    String setFileName = "http_1000.set";
    setupSecondDefensics(setFileName);

    List<String> expectedTabNames = new ArrayList<>();
    expectedTabNames.add(SETTING_FILE_PATH);
    expectedTabNames.add(setFileName);

    try (JenkinsRule.WebClient webClient = jenkinsRule.createWebClient()) {
      FreeStyleBuild run = project.scheduleBuild2(0).get();

      assertThat(run.getActions(HtmlReportAction.class).size(), is(equalTo(1)));
      doAssertionsOnReport(
          webClient.getPage(project),
          expectedTabNames);
      doAssertionsOnReport(
          webClient.getPage(run),
          expectedTabNames);
    }
  }

  private void setupSecondDefensics(String setFileName) throws Exception {
    // Set up second build step with different Defensics configuration
    String name2 = "My Other Defensics";
    ProjectUtils.addInstanceConfiguration(jenkinsRule, name2, LOCAL_URL,
        true, credentialsId);
    ProjectUtils.addBuildStep(project, name2, setFileName, false);
    ProjectUtils.copyFileToWorkspace(
        jenkinsRule,
        project,
        setFileName);
  }

  private void doAssertionsOnReport(HtmlPage page, List<String> expectedTabNames)
      throws IOException {
    HtmlAnchor resultLink = page.getAnchorByText(ResultPublisher.REPORT_NAME);
    Page resultPage = resultLink.openLinkInNewWindow();

    assertThat(resultPage.isHtmlPage(), is(true));
    assertThat(resultLink, is(not(nullValue())));

    for (int i = 0; i < expectedTabNames.size(); i++) {
      DomElement resultTab = ((HtmlPage) resultPage).getElementById("tab" + (i + 1));
      assertThat("Result tab " + (i + 1) + " does not exist.", resultTab != null);
      assertThat(
          "Tab " + (i + 1) + " has an unexpected title",
          resultTab.getTextContent(),
          is(equalTo(expectedTabNames.get(i))));
    }

    DomElement resultTab =
        ((HtmlPage) resultPage).getElementById("tab" + (expectedTabNames.size() + 1));
    assertThat(resultTab, is(nullValue()));
  }

  @Test
  public void testTrendGraph() throws Exception {
    DefensicsMockServer defensicsMockServer = new DefensicsMockServer(false, "PASS", RunState.COMPLETED);
    defensicsMockServer.initServer(ResultIT.mockServer);

    try (JenkinsRule.WebClient webClient = jenkinsRule.createWebClient()) {
      project.scheduleBuild2(0).get();
      project.scheduleBuild2(0).get();
      HtmlPage projectPage = webClient.getPage(project);

      assertThat(project.getActions(ProjectHistoryAction.class).size(), is(equalTo(1)));
      assertThat(
          projectPage.getElementById("defensics-failure-trend-title"),
          is(not(nullValue())));
      assertThat(
          projectPage.getElementById("defensics-failure-trend-graph"),
          is(not(nullValue())));
    }
  }

  @Test
  public void testTrendGraphSomeBuildsHaveNoDefensicsResults() throws Exception {
    DefensicsMockServer defensicsMockServer = new DefensicsMockServer(false, "PASS", RunState.COMPLETED);
    defensicsMockServer.initServer(ResultIT.mockServer);

    try (JenkinsRule.WebClient webClient = jenkinsRule.createWebClient()) {
      project.scheduleBuild2(0).get();
      project.getBuildersList().clear();
      project.scheduleBuild2(0).get();
      project.scheduleBuild2(0).get();
      HtmlPage projectPage = webClient.getPage(project);

      assertThat(project.getActions(ProjectHistoryAction.class).size(), is(equalTo(1)));
      assertThat(
          projectPage.getElementById("defensics-failure-trend-title"),
          is(not(nullValue())));
      assertThat(
          projectPage.getElementById("defensics-failure-trend-graph"),
          is(not(nullValue())));
    }
  }

  @Test
  public void testTrendGraphNotShownWhenTooFewResults() throws Exception {
    DefensicsMockServer defensicsMockServer = new DefensicsMockServer(false, "PASS", RunState.COMPLETED);
    defensicsMockServer.initServer(ResultIT.mockServer);

    try (JenkinsRule.WebClient webClient = jenkinsRule.createWebClient()) {
      project.scheduleBuild2(0).get();
      HtmlPage projectPage = webClient.getPage(project);

      assertThat(project.getActions(ProjectHistoryAction.class).size(), is(equalTo(1)));
      assertThat(
          projectPage.getElementById("defensics-failure-trend-title"),
          is(nullValue()));
      assertThat(
          projectPage.getElementById("defensics-failure-trend-graph"),
          is(nullValue()));
    }
  }

  @Test
  public void testResultPackagePublishIsDisabledByDefault() throws Exception {
    DefensicsMockServer defensicsMockServer = new DefensicsMockServer(false, "PASS", RunState.COMPLETED);
    defensicsMockServer.initServer(ResultIT.mockServer);

    try (JenkinsRule.WebClient webClient = jenkinsRule.createWebClient()) {
      final FreeStyleBuild build = project.scheduleBuild2(0).get();
      final HtmlPage buildPage = webClient.getPage(build);

      assertThat(buildPage.getElementById("defensics-results-package-list"), is(nullValue()));
    }
  }

  @Test
  public void testResultPackagePublish() throws Exception {
    DefensicsMockServer defensicsMockServer = new DefensicsMockServer(false, "PASS", RunState.COMPLETED);
    defensicsMockServer.initServer(ResultIT.mockServer);

    ProjectUtils.addBuildStep(project, NAME, SETTING_FILE_PATH, true);

    try (JenkinsRule.WebClient webClient = jenkinsRule.createWebClient()) {
      final FreeStyleBuild build = project.scheduleBuild2(0).get();
      final HtmlPage buildPage = webClient.getPage(build);

      final DomElement resultPackageList = buildPage.getElementById("defensics-results-package-list");
      assertThat(resultPackageList, is(notNullValue()));
      assertThat(resultPackageList.getChildElementCount(), is(1));
    }
  }

  @Test
  public void testMultipleResultPackages() throws Exception {
    DefensicsMockServer defensicsMockServer = new DefensicsMockServer(false, "PASS", RunState.COMPLETED);
    defensicsMockServer.initServer(ResultIT.mockServer);

    ProjectUtils.addBuildStep(project, NAME, SETTING_FILE_PATH, true);
    ProjectUtils.addPostBuildStep(project, NAME, SETTING_FILE_PATH, true);

    try (JenkinsRule.WebClient webClient = jenkinsRule.createWebClient()) {
      final FreeStyleBuild build = project.scheduleBuild2(0).get();
      final HtmlPage buildPage = webClient.getPage(build);

      final DomElement resultPackageList = buildPage.getElementById("defensics-results-package-list");
      assertThat(resultPackageList, is(notNullValue()));
      assertThat(resultPackageList.getChildElementCount(), is(2));
    }
  }
}
