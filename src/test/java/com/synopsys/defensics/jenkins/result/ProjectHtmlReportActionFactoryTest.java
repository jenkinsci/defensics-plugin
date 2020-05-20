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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;

import com.synopsys.defensics.jenkins.result.HtmlReportPublisherTarget.HtmlReportAction;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ProjectHtmlReportActionFactoryTest {

  @Mock
  private FreeStyleProject project;
  @Mock
  private HtmlReportAction reportAction;
  @Mock
  private FreeStyleBuild run;

  private ProjectHtmlReportActionFactory factory;

  @Before
  public void setup() {
    factory = new ProjectHtmlReportActionFactory();

    when(project.getLastCompletedBuild()).thenReturn(run);
    when(run.getActions(HtmlReportAction.class)).thenReturn(Collections.singletonList(
        reportAction));
  }

  @Test
  public void testCreation() {
    assertThat(factory, is(notNullValue()));
  }

  @Test
  public void testGetType() {
    assertThat(factory.type(), is(equalTo(Job.class)));
  }

  @Test
  public void testCreateAction() {
    assertThat(factory.createFor(project), contains(reportAction));
  }
}
