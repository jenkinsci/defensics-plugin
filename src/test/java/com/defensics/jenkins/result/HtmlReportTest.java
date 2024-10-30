/*
 * Copyright 2024 Black Duck Software, Inc. All rights reserved.
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

package com.defensics.jenkins.result;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import hudson.FilePath;
import java.io.File;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class HtmlReportTest {

  public static final String JOB_ID = "1";
  public static final String REPORT_TITLE = "my testplan";
  private HtmlReport report;

  @Before
  public void setup() throws Exception {
    TemporaryFolder temporaryFolder = new TemporaryFolder();
    temporaryFolder.create();
    FilePath workspace = new FilePath(temporaryFolder.getRoot());

    // "Download" the report to result folder
    FilePath resultFolder = workspace.child("results");
    resultFolder.mkdirs();
    resultFolder.child("report.html").copyFrom(
        new FilePath(new File("src/test/resources/com/defensics/jenkins/result.html")));

    report = new HtmlReport(resultFolder, JOB_ID, REPORT_TITLE);
  }

  @Test
  public void testCreation() {
    assertThat(report, is(notNullValue()));
  }

  @Test
  public void testGetFileName() {
    assertThat(report.getFileName(), is(equalTo("report-" + JOB_ID + ".html")));
  }

  @Test
  public void testGetReportTitle() {
    assertThat(report.getReportTitle(), is(equalTo(REPORT_TITLE)));
  }

  @Test
  public void testDelete() throws Exception {
    report.delete();
    assertThat(report.exists(), is(false));
  }
}
