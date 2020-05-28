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

package com.synopsys.defensics.jenkins.result;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInRelativeOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import org.junit.Before;
import org.junit.Test;

public class BuildResultActionTest {

  private static final String REPORT_URL = "https://example.com/report.html";
  private static final long FAILURE_COUNT = 1234567890L;
  private static final String RESULT_PACKAGE_URL = "https://example.com/result1.zip";
  private static final String RESULT_PACKAGE_URL2 = "https://example.com/result2.zip";
  private BuildResultAction buildResultAction;

  @Before
  public void setup() {
    buildResultAction = new BuildResultAction(REPORT_URL, FAILURE_COUNT, RESULT_PACKAGE_URL);
  }

  @Test
  public void testCreation() {
    assertThat(buildResultAction, is(not(nullValue())));
  }

  @Test
  public void testGetReportUrl() {
    assertThat(buildResultAction.getReportUrl(), is(equalTo(REPORT_URL)));
  }

  @Test
  public void testGetFailureCount() {
    assertThat(buildResultAction.getFailureCount(), is(equalTo(FAILURE_COUNT)));
  }

  @Test
  public void testGetResultPackageUrl() {
    assertThat(buildResultAction.getResultPackages(), contains(RESULT_PACKAGE_URL));
  }

  @Test
  public void testGetResultPackageUrls() {
    assertThat(buildResultAction.getResultPackages(), contains(RESULT_PACKAGE_URL));
    assertThat(buildResultAction.getResultPackages(), not(contains(RESULT_PACKAGE_URL2)));
    buildResultAction.addResultPackage(RESULT_PACKAGE_URL2);
    assertThat(buildResultAction.getResultPackages(),
        containsInRelativeOrder(RESULT_PACKAGE_URL, RESULT_PACKAGE_URL2));
  }
}
