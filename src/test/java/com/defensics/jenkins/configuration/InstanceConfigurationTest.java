/*
 * Copyright 2024 Black Duck Software, Inc. All rights reserved.
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

package com.defensics.jenkins.configuration;

import static com.defensics.jenkins.test.utils.Constants.CERTIFICATE_VALIDATION_ENABLED;
import static com.defensics.jenkins.test.utils.Constants.CREDENTIALS_ID;
import static com.defensics.jenkins.test.utils.Constants.NAME;
import static com.defensics.jenkins.test.utils.Constants.URL;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Before;
import org.junit.Test;

public class InstanceConfigurationTest {

  private InstanceConfiguration configuration;

  @Before
  public void setup() {
    configuration = new InstanceConfiguration(NAME, URL, CERTIFICATE_VALIDATION_ENABLED,
        CREDENTIALS_ID);
  }

  @Test
  public void testGetName() {
    assertThat(configuration.getName(), is(equalTo(NAME)));
  }

  @Test
  public void testGetUrl() {
    assertThat(configuration.getUrl(), is(equalTo(URL)));
  }

  @Test
  public void testGetCertificateValidationDisabled() {
    assertThat(configuration.isCertificateValidationDisabled(),
        is(equalTo(CERTIFICATE_VALIDATION_ENABLED)));
  }

  @Test
  public void testGetCredentialsId() {
    assertThat(configuration.getCredentialsId(), is(equalTo(CREDENTIALS_ID)));
  }

  @Test
  public void testGetDisplayName() {
    assertThat(configuration.getDisplayName(), containsString(NAME));
    assertThat(configuration.getDisplayName(), containsString(URL));
  }

  @Test
  public void testEqualsAndHashCode() {
    EqualsVerifier.forClass(InstanceConfiguration.class).verify();
  }
}
