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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.synopsys.defensics.api.ApiService;
import com.synopsys.defensics.apiserver.model.RunState;
import com.synopsys.defensics.jenkins.result.HtmlReportPublisherTarget.HtmlReportAction;
import com.synopsys.defensics.jenkins.result.ResultPublisher;
import com.synopsys.defensics.jenkins.result.history.ProjectHistoryAction;
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

  public static final String NAME = "My Defensics";
  public static final String URL = "http://localhost:1080/";
  public static final boolean CERTIFICATE_VALIDATION_DISABLED = false;
  public static final String CREDENTIALSID = "";
  public static final String TESTPLAN_NAME = "http.testplan";
  private static ClientAndServer mockServer;
  @Rule
  public JenkinsRule jenkinsRule = new JenkinsRule();
  private ApiService api;
  private FreeStyleProject project;

  @Before
  public void setup() throws Exception {
    api = new ApiService(URL, "", CERTIFICATE_VALIDATION_DISABLED);

    project = jenkinsRule.createFreeStyleProject();
    ProjectUtils.setupProject(
        jenkinsRule,
        project,
        NAME,
        URL,
        CERTIFICATE_VALIDATION_DISABLED,
        CREDENTIALSID,
        TESTPLAN_NAME);
    ProjectUtils.addBuildStep(project, NAME, TESTPLAN_NAME);

    EnvironmentVariablesNodeProperty prop = new EnvironmentVariablesNodeProperty();
    EnvVars env = prop.getEnvVars();
    env.put("DEFENSICS_MAX_POLLING_INTERVAL", "1");
    jenkinsRule.jenkins.getGlobalNodeProperties().add(prop);
  }

  @After
  public void stopServer() {
    mockServer.stop();
  }

  @Test
  public void testPublishReport() throws Exception {
    mockServer = ClientAndServer.startClientAndServer(1080);
    DefensicsMockServer mockServer = new DefensicsMockServer(false, "PASS", RunState.COMPLETED);
    mockServer.initServer(ResultIT.mockServer);

    try (JenkinsRule.WebClient webClient = jenkinsRule.createWebClient()) {
      List<String> expectedTabNames = Collections.singletonList(TESTPLAN_NAME);

      FreeStyleBuild run = project.scheduleBuild2(0).get();

      assertThat(run.getActions(HtmlReportAction.class).size(), is(equalTo(1)));
      doAssertionsOnReport(
          webClient.getPage(project),
          ResultPublisher.REPORT_NAME,
          expectedTabNames);
      doAssertionsOnReport(
          webClient.getPage(run),
          ResultPublisher.REPORT_NAME,
          expectedTabNames);
    }
  }

  @Test
  public void testPublishReportWithFailures() throws Exception {
    mockServer = ClientAndServer.startClientAndServer(1080);
    DefensicsMockServer mockServer = new DefensicsMockServer(false, "FAIL", RunState.COMPLETED);
    mockServer.initServer(ResultIT.mockServer);

    try (JenkinsRule.WebClient webClient = jenkinsRule.createWebClient()) {
      List<String> expectedTabNames = Collections.singletonList(TESTPLAN_NAME);

      FreeStyleBuild run = project.scheduleBuild2(0).get();

      assertThat(run.getActions(HtmlReportAction.class).size(), is(equalTo(1)));
      doAssertionsOnReport(
          webClient.getPage(project),
          ResultPublisher.REPORT_NAME,
          expectedTabNames);
      doAssertionsOnReport(
          webClient.getPage(run),
          ResultPublisher.REPORT_NAME,
          expectedTabNames);
    }
  }

  @Test
  public void testPublishMultipleReports() throws Exception {
    mockServer = ClientAndServer.startClientAndServer(1080);
    DefensicsMockServer mockServer = new DefensicsMockServer(false, "PASS", RunState.COMPLETED);
    mockServer.initServer(ResultIT.mockServer);

    String setFileName = "http_1000.set";
    setupSecondDefensics(setFileName);

    List<String> expectedTabNames = new ArrayList();
    expectedTabNames.add(TESTPLAN_NAME);
    expectedTabNames.add(setFileName);

    try (JenkinsRule.WebClient webClient = jenkinsRule.createWebClient()) {
      FreeStyleBuild run = project.scheduleBuild2(0).get();

      assertThat(run.getActions(HtmlReportAction.class).size(), is(equalTo(1)));
      doAssertionsOnReport(
          webClient.getPage(project),
          ResultPublisher.REPORT_NAME,
          expectedTabNames);
      doAssertionsOnReport(
          webClient.getPage(run),
          ResultPublisher.REPORT_NAME,
          expectedTabNames);
    }
  }

  private void setupSecondDefensics(String setFileName) throws Exception {
    // Set up second build step with different Defensics configuration
    String name2 = "My Other Defensics";
    String url2 = "http://localhost:1080/";
    String credentialsId2 = "";
    ProjectUtils.addInstanceConfiguration(jenkinsRule, name2, url2,
        true, credentialsId2);
    ProjectUtils.addBuildStep(project, name2, setFileName);
    ProjectUtils.copyFileToWorkspace(
        jenkinsRule,
        project,
        setFileName);
  }

  private void doAssertionsOnReport(
      HtmlPage page, String reportLinkText, List<String> expectedTabNames)
      throws IOException {
    HtmlAnchor resultLink = page.getAnchorByText(reportLinkText);
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
    mockServer = ClientAndServer.startClientAndServer(1080);
    DefensicsMockServer mockServer = new DefensicsMockServer(false, "PASS", RunState.COMPLETED);
    mockServer.initServer(ResultIT.mockServer);

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
    mockServer = ClientAndServer.startClientAndServer(1080);
    DefensicsMockServer mockServer = new DefensicsMockServer(false, "PASS", RunState.COMPLETED);
    mockServer.initServer(ResultIT.mockServer);

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
    mockServer = ClientAndServer.startClientAndServer(1080);
    DefensicsMockServer mockServer = new DefensicsMockServer(false, "PASS", RunState.COMPLETED);
    mockServer.initServer(ResultIT.mockServer);

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
}
