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

import static com.synopsys.defensics.jenkins.test.utils.Constants.CONFIGURATION_OVERRIDES;
import static com.synopsys.defensics.jenkins.test.utils.Constants.NAME;
import static com.synopsys.defensics.jenkins.test.utils.Constants.SETTING_FILE_PATH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.Before;
import org.junit.Test;
public class FuzzBuildStepTest {

  FuzzBuildStep fuzzBuildStep;

  @Before
  public void setup() {
    fuzzBuildStep = new FuzzBuildStep(SETTING_FILE_PATH);
    fuzzBuildStep.setDefensicsInstance(NAME);
    fuzzBuildStep.setConfigurationOverrides(CONFIGURATION_OVERRIDES);
  }

  @Test
  public void testCreation() {
    assertThat(fuzzBuildStep, is(notNullValue()));
  }

  @Test
  public void testGetDefensicsConfigurationName() {
    assertThat(fuzzBuildStep.getDefensicsInstance(), is(equalTo(NAME)));
  }

  @Test
  public void testGetConfigurationOverrides() {
    assertThat(fuzzBuildStep.getConfigurationOverrides(), is(equalTo(CONFIGURATION_OVERRIDES)));
  }

  @Test
  public void testGetSettingFilePath() {
    assertThat(fuzzBuildStep.getConfigurationFilePath(), is(equalTo(SETTING_FILE_PATH)));
  }
}
