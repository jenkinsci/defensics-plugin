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

package com.synopsys.defensics.jenkins.configuration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Before;
import org.junit.Test;

public class InstanceConfigurationTest {

  public static final String CONFIGURATION_NAME = "Name of my Defensics";
  public static final String CONFIGURATION_URL = "http://url.to.defensics";
  public static final boolean CERTIFICATE_VALIDATION_DISABLED = false;
  public static final String CONFIGURATION_CREDENTIALSID = "test-credential";

  private InstanceConfiguration configuration;

  @Before
  public void setup() {
    configuration = new InstanceConfiguration(CONFIGURATION_NAME, CONFIGURATION_URL,
        CERTIFICATE_VALIDATION_DISABLED, CONFIGURATION_CREDENTIALSID);
  }

  @Test
  public void testGetName() {
    assertThat(configuration.getName(), is(equalTo(CONFIGURATION_NAME)));
  }

  @Test
  public void testGetUrl() {
    assertThat(configuration.getUrl(), is(equalTo(CONFIGURATION_URL)));
  }

  @Test
  public void testGetCertificateValidationDisabled() {
    assertThat(
        configuration.isCertificateValidationDisabled(),
        is(equalTo(CERTIFICATE_VALIDATION_DISABLED)));
  }

  @Test
  public void testGetCredentialsId() {
    assertThat(configuration.getCredentialsId(), is(equalTo(CONFIGURATION_CREDENTIALSID)));
  }

  @Test
  public void testGetDisplayName() {
    assertThat(configuration.getDisplayName(), containsString(CONFIGURATION_NAME));
    assertThat(configuration.getDisplayName(), containsString(CONFIGURATION_URL));
  }

  @Test
  public void testEqualsAndHashCode() {
    EqualsVerifier.forClass(InstanceConfiguration.class).verify();
  }
}
