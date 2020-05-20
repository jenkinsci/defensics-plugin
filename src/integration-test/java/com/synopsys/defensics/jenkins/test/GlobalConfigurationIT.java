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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.synopsys.defensics.jenkins.FuzzBuildStep;
import com.synopsys.defensics.jenkins.configuration.InstanceConfiguration;
import com.synopsys.defensics.jenkins.configuration.PluginConfiguration;
import hudson.util.ListBoxModel;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class GlobalConfigurationIT {

  private static final String NAME = "My Defensics";
  private static final String URL = "http://my.defensics";
  private static final boolean CERTIFICATE_VALIDATION_DISABLED = true;
  private static final String CREDENTIAL_ID = "test-credentials";
  private static final String SETTING_FILE_PATH = "http.testplan";

  @Rule
  public JenkinsRule jenkinsRule = new JenkinsRule();

  private FuzzBuildStep plugin;
  private InstanceConfiguration instanceConfiguration;
  private List<InstanceConfiguration> defensicsInstances;
  private PluginConfiguration pluginConfiguration;

  @Before
  public void setup() {
    defensicsInstances = new ArrayList<>();
    instanceConfiguration = new InstanceConfiguration(
        NAME, URL, CERTIFICATE_VALIDATION_DISABLED, CREDENTIAL_ID);
    defensicsInstances.add(instanceConfiguration);
    pluginConfiguration = jenkinsRule.get(PluginConfiguration.class);
    pluginConfiguration.setDefensicsInstances(defensicsInstances);
    plugin = new FuzzBuildStep(SETTING_FILE_PATH);
    plugin.setDefensicsInstance(NAME);
  }

  @Test
  public void testRoundTrip() throws Exception {
    jenkinsRule.configRoundtrip();

    assertThat(
        pluginConfiguration.getDefensicsInstances().size(),
        is(equalTo(defensicsInstances.size())));
    assertThat(
        pluginConfiguration.getDefensicsInstances().get(0),
        is(equalTo(defensicsInstances.get(0))));
  }

  @Test
  public void testMissingName() throws Exception {
    instanceConfiguration = new InstanceConfiguration(
        null, URL, CERTIFICATE_VALIDATION_DISABLED, CREDENTIAL_ID);
    defensicsInstances = new ArrayList<>();
    defensicsInstances.add(instanceConfiguration);
    pluginConfiguration.setDefensicsInstances(defensicsInstances);

    try {
      jenkinsRule.configRoundtrip();
      fail("Saving global configuration should fail.");
    } catch (FailingHttpStatusCodeException exception) {
      assertThat(
          exception.getResponse().getContentAsString(),
          containsString("Defensics instance name is empty"));
    }
  }

  @Test
  public void testDuplicateName() throws Exception {
    defensicsInstances.add(instanceConfiguration);
    pluginConfiguration.setDefensicsInstances(defensicsInstances);

    try {
      jenkinsRule.configRoundtrip();
      fail("Saving global configuration should fail.");
    } catch (FailingHttpStatusCodeException exception) {
      assertThat(
          exception.getResponse().getContentAsString(),
          containsString("The Defensics instance name is already configured: " + NAME));
    }
  }

  @Test
  public void testMissingUrl() throws Exception {
    instanceConfiguration = new InstanceConfiguration(
        NAME, null, CERTIFICATE_VALIDATION_DISABLED, CREDENTIAL_ID);
    defensicsInstances = new ArrayList<>();
    defensicsInstances.add(instanceConfiguration);
    pluginConfiguration.setDefensicsInstances(defensicsInstances);

    try {
      jenkinsRule.configRoundtrip();
      fail("Saving global configuration should fail.");
    } catch (FailingHttpStatusCodeException exception) {
      assertThat(
          exception.getResponse().getContentAsString(),
          containsString("Defensics instance URL is empty"));
    }
  }

  @Test
  public void testInvalidUrl() throws Exception {
    instanceConfiguration = new InstanceConfiguration(NAME, "not_a_valid_url",
        CERTIFICATE_VALIDATION_DISABLED, CREDENTIAL_ID);
    defensicsInstances = new ArrayList<>();
    defensicsInstances.add(instanceConfiguration);
    pluginConfiguration.setDefensicsInstances(defensicsInstances);

    try {
      jenkinsRule.configRoundtrip();
      fail("Saving global configuration should fail.");
    } catch (FailingHttpStatusCodeException exception) {
      assertThat(
          exception.getResponse().getContentAsString(),
          containsString("Defensics instance URL is not valid"));
    }
  }

  @Test
  public void testDefensicsConfigurationNameItems() {
    InstanceConfiguration instanceConfiguration2 =
        new InstanceConfiguration("Other instance", "http://other.url",
            CERTIFICATE_VALIDATION_DISABLED, CREDENTIAL_ID);
    defensicsInstances.add(instanceConfiguration2);
    pluginConfiguration.setDefensicsInstances(defensicsInstances);

    ListBoxModel listBoxModel =
        pluginConfiguration.doFillDefensicsInstanceNameItems();

    assertThat(listBoxModel.get(0).name, is(equalTo(instanceConfiguration.getDisplayName())));
    assertThat(listBoxModel.get(0).value, is(equalTo(instanceConfiguration.getName())));
    assertThat(listBoxModel.get(1).name, is(equalTo(instanceConfiguration2.getDisplayName())));
    assertThat(listBoxModel.get(1).value, is(equalTo(instanceConfiguration2.getName())));

  }

}
