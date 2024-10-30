/*
 * Copyright © 2020-2023 Synopsys, Inc.
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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Before;
import org.junit.Test;

public class HtmlReportPublisherTargetTest {

  private static final String REPORT_NAME = "";
  private static final String REPORT_DIR = "";
  private static final String REPORT_FILES = "";
  private static final String JOB_ID = "1";
  private static final String REPORT_TITLES = "";
  private HtmlReportPublisherTarget target;

  @Before
  public void setup() {
    target = new HtmlReportPublisherTarget(
        REPORT_NAME,
        REPORT_DIR,
        REPORT_FILES,
        REPORT_TITLES,
        JOB_ID);
  }

  @Test
  public void testCreation() {
    assertThat(target, is(notNullValue()));
  }

  @Test
  public void testGetJobId() {
    assertThat(target.getJobId(), is(equalTo(JOB_ID)));
  }

  @Test
  public void testEqualsAndHashCode() {
    EqualsVerifier.forClass(HtmlReportPublisherTarget.class)
        .suppress(Warning.NONFINAL_FIELDS)
        .withRedefinedSuperclass()
        .withIgnoredFields(
            "reportTitles",
            "includes",
            "escapeUnderscores",
            "useWrapperFileDirectly",
            "numberOfWorkers"
        )
        .verify();
  }
}
