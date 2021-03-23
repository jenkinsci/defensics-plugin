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

import static com.synopsys.defensics.jenkins.test.utils.Constants.CERTIFICATE_VALIDATION_ENABLED;
import static com.synopsys.defensics.jenkins.test.utils.Constants.LOCAL_URL;
import static com.synopsys.defensics.jenkins.test.utils.Constants.NAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

import com.synopsys.defensics.apiserver.model.RunState;
import com.synopsys.defensics.jenkins.configuration.InstanceConfiguration.DefensicsInstanceConfigurationDescriptor;
import com.synopsys.defensics.jenkins.test.utils.CredentialsUtil;
import com.synopsys.defensics.jenkins.test.utils.DefensicsMockServer;
import hudson.model.Item;
import hudson.util.FormValidation;
import hudson.util.FormValidation.Kind;
import hudson.util.ListBoxModel;
import java.io.IOException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mockito;
import org.mockserver.integration.ClientAndServer;

public class InstanceConfigurationIT {

  @Rule
  public JenkinsRule jenkinsRule = new JenkinsRule();

  private DefensicsInstanceConfigurationDescriptor defensicsInstanceConfigurationDescriptor;
  private String credentialsId;

  @Before
  public void setup() throws IOException {
    defensicsInstanceConfigurationDescriptor =
        jenkinsRule.get(DefensicsInstanceConfigurationDescriptor.class);
    credentialsId = CredentialsUtil.createValidCredentials(jenkinsRule.jenkins);
  }

  @Test
  public void testCheckName() {
    assertThat(
        defensicsInstanceConfigurationDescriptor.doCheckName(NAME).kind, is(equalTo(Kind.OK)));
  }

  @Test
  public void testCheckNameMissing() {
    FormValidation result = defensicsInstanceConfigurationDescriptor.doCheckName("");

    assertThat(result.getMessage(), is(equalTo("Name is not defined")));
    assertThat(result.kind, is(Kind.ERROR));
  }

  @Test
  public void testCheckUrl() {
    assertThat(defensicsInstanceConfigurationDescriptor.doCheckUrl(LOCAL_URL).kind, is(equalTo(Kind.OK)));
  }

  @Test
  public void testCheckUrlMissing() {
    FormValidation result = defensicsInstanceConfigurationDescriptor.doCheckUrl("");

    assertThat(result.getMessage(), is(equalTo("URL is not defined")));
    assertThat(result.kind, is(Kind.ERROR));
  }

  @Test
  public void testCheckUrlInvalid() {
    FormValidation result = defensicsInstanceConfigurationDescriptor.doCheckUrl("not_a_valid_url");

    assertThat(result.kind, is(Kind.ERROR));
  }

  @Test
  public void testCredentialItem() {
    Item item = Mockito.mock(Item.class);
    ListBoxModel listBoxModel =
        defensicsInstanceConfigurationDescriptor.doFillCredentialsIdItems(item, credentialsId);

    assertThat(listBoxModel.get(0).value, is(equalTo(credentialsId)));
  }

  @Test
  public void testConnectionTest() {
    ClientAndServer mockServer = ClientAndServer.startClientAndServer(1080);
    try {
      DefensicsMockServer defensicsMockServer = new DefensicsMockServer(
          true, "PASS", RunState.COMPLETED
      );
      defensicsMockServer.initServer(mockServer);

      FormValidation result = defensicsInstanceConfigurationDescriptor.doTestConnection(
          LOCAL_URL, CERTIFICATE_VALIDATION_ENABLED, credentialsId);

      assertThat(result.kind, is(equalTo(Kind.OK)));
    } finally {
      DefensicsMockServer.stopMockServer(mockServer);
    }
  }

  @Test
  public void testConnectionTestInvalidUrl() {
    FormValidation result = defensicsInstanceConfigurationDescriptor.doTestConnection(
        "not_a_valid_url", CERTIFICATE_VALIDATION_ENABLED, "");
    assertThat(result.kind, is(equalTo(Kind.ERROR)));
    assertThat(result.getMessage(),
        is(equalTo("Failed to connect to server: no protocol: not_a_valid_url")));
  }

  @Test
  public void testConnectionTestUnreachableUrl() {
    FormValidation result = defensicsInstanceConfigurationDescriptor.doTestConnection(
        LOCAL_URL, CERTIFICATE_VALIDATION_ENABLED, "");
    assertThat(result.kind, is(equalTo(Kind.ERROR)));
    assertThat(
        result.getMessage(),
        startsWith("Failed to connect to server:"));
  }

  @Test
  public void testConnectionTestInvalidCredential() {
    FormValidation result = defensicsInstanceConfigurationDescriptor.doTestConnection(
        LOCAL_URL, CERTIFICATE_VALIDATION_ENABLED, "invalid-id");
    assertThat(result.kind, is(equalTo(Kind.ERROR)));
    assertThat(result.getMessage(), is(equalTo(
        "ERROR: Cannot find credential: &#039;invalid-id&#039;")));
  }
}
