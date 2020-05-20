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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.synopsys.defensics.jenkins.configuration.AuthenticationTokenProvider;
import hudson.util.Secret;
import java.io.IOException;
import java.net.URL;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class AuthenticationTokenIT {

  private static final String CREDENTIAL_ID = "test-credentials";
  private static final String SECRET_TOKEN = "test-token";

  @Rule
  public JenkinsRule jenkinsRule = new JenkinsRule();

  private CredentialsStore store;

  @Before
  public void setup() {
    store = CredentialsProvider.lookupStores(jenkinsRule.jenkins).iterator().next();
  }

  @Test
  public void testCredential() throws IOException {
    StringCredentialsImpl credential = new StringCredentialsImpl(
        CredentialsScope.GLOBAL,
        CREDENTIAL_ID,
        "Test Secret Text",
        Secret.fromString(SECRET_TOKEN));

    store.addCredentials(Domain.global(), credential);
    assertThat(AuthenticationTokenProvider.getAuthenticationToken(
        new URL("http://www.doesnt.matter"), CREDENTIAL_ID), is(equalTo(SECRET_TOKEN)));


  }
}
