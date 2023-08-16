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

import static com.synopsys.defensics.jenkins.test.utils.Constants.SETTING_FILE_PATH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import com.synopsys.defensics.jenkins.FuzzBuildStep;
import com.synopsys.defensics.jenkins.FuzzBuildStep.FuzzBuildStepDescriptor;
import com.synopsys.defensics.jenkins.configuration.InstanceConfiguration;
import com.synopsys.defensics.jenkins.configuration.PluginConfiguration;
import hudson.util.FormValidation.Kind;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class JobConfigurationIT {

  public final InstanceConfiguration configuration1 =
      new InstanceConfiguration("My Defensics", "http://my.defensics",
          true, "test-credentials");
  public final InstanceConfiguration configuration2 =
      new InstanceConfiguration("My Other Defensics", "http://my.other.defensics",
          true, "test-credentials2");
  @Rule
  public final JenkinsRule jenkinsRule = new JenkinsRule();
  private FuzzBuildStep fuzzBuildStep;
  private FuzzBuildStepDescriptor stepConfigurationDescriptor;

  @Before
  public void setup() {
    fuzzBuildStep = new FuzzBuildStep(SETTING_FILE_PATH);
    fuzzBuildStep.setDefensicsInstance(configuration1.getName());

    final List<InstanceConfiguration> defensicsInstances = new ArrayList<>();
    defensicsInstances.add(configuration1);
    defensicsInstances.add(configuration2);

    final PluginConfiguration pluginConfiguration = jenkinsRule.get(PluginConfiguration.class);
    pluginConfiguration.setDefensicsInstances(defensicsInstances);

    stepConfigurationDescriptor =
        jenkinsRule.get(FuzzBuildStepDescriptor.class);
  }

  @Test
  public void testGetDefensicsConfigurationName() {
    assertThat(
        fuzzBuildStep.getDefensicsInstance(),
        is(equalTo(configuration1.getName())));
  }

  @Test
  public void testGetDefensicsConfigurationNameNotSet() {
    // Create new fuzzBuildStep instance and don't set configuration name
    fuzzBuildStep = new FuzzBuildStep(SETTING_FILE_PATH);

    assertThat(fuzzBuildStep.getDefensicsInstance(), is(nullValue()));
  }

  @Test
  public void testRoundTripDefensicsConfigurationName() throws Exception {
    // Create a project using this build step, open the configuration form, and save it
    jenkinsRule.configRoundtrip(fuzzBuildStep);

    // Assert that the correct Defensics instance is still configured
    assertThat(
        fuzzBuildStep.getDefensicsInstance(),
        is(equalTo(configuration1.getName())));
  }

  @Test
  public void testSettingFilePathValidation() {
    assertThat(
        stepConfigurationDescriptor.doCheckConfigurationFilePath(SETTING_FILE_PATH).kind,
        is(Kind.OK));
  }

  @Test
  public void testSettingFilePathValidationWithEmpty() {
    assertThat(
        stepConfigurationDescriptor.doCheckConfigurationFilePath("").kind, is(Kind.ERROR));
  }

  @Test
  public void testSettingFilePathValidationWithWrongExtension() {
    assertThat(
        stepConfigurationDescriptor.doCheckConfigurationFilePath("somefile.txt").kind,
        is(Kind.ERROR));
  }
}
