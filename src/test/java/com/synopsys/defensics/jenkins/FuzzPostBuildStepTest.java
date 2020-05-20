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

package com.synopsys.defensics.jenkins;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import org.junit.Before;
import org.junit.Test;

public class FuzzPostBuildStepTest {

  private static final String SETTING_FILE_PATH = "my_plan.set";
  private static final String NAME = "My Defensics";
  private static final String CONFIGURATION_OVERRIDES = "--test.setting=thisIsFakeSetting";

  private FuzzPostBuildStep postBuildStep;

  @Before
  public void setup() {
    postBuildStep = new FuzzPostBuildStep(SETTING_FILE_PATH);
    postBuildStep.setDefensicsInstance(NAME);
    postBuildStep.setConfigurationOverrides(CONFIGURATION_OVERRIDES);
  }

  @Test
  public void testCreation() {
    assertThat(postBuildStep, is(not(nullValue())));
  }

  @Test
  public void testGetDefensicsConfigurationName() {
    assertThat(postBuildStep.getDefensicsInstance(), is(equalTo(NAME)));
  }

  @Test
  public void testGetConfigurationOverrides() {
    assertThat(postBuildStep.getConfigurationOverrides(), is(equalTo(CONFIGURATION_OVERRIDES)));
  }

  @Test
  public void testGetSettingFilePath() {
    assertThat(postBuildStep.getConfigurationFilePath(), is(equalTo(SETTING_FILE_PATH)));
  }
}
