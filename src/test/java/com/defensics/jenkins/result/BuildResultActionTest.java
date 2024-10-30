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

package com.defensics.jenkins.result;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.Before;
import org.junit.Test;

public class BuildResultActionTest {

  private static final String REPORT_URL = "https://example.com/report.html";
  private static final long FAILURE_COUNT = 1234567890L;
  private BuildResultAction buildResultAction;

  @Before
  public void setup() {
    buildResultAction = new BuildResultAction(REPORT_URL, FAILURE_COUNT);
  }

  @Test
  public void testCreation() {
    assertThat(buildResultAction, is(notNullValue()));
  }

  @Test
  public void testGetReportUrl() {
    assertThat(buildResultAction.getReportUrl(), is(REPORT_URL));
  }

  @Test
  public void testGetFailureCount() {
    assertThat(buildResultAction.getFailureCount(), is(FAILURE_COUNT));
  }

  @Test
  public void testSetFailureCount() {
    assertThat(buildResultAction.getFailureCount(), is(FAILURE_COUNT));
    final long newFailureCount = 2 * FAILURE_COUNT;
    buildResultAction.setFailureCount(newFailureCount);
    assertThat(buildResultAction.getFailureCount(), is(newFailureCount));
  }
}
