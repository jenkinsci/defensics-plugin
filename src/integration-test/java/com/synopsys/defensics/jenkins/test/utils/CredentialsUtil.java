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

package com.synopsys.defensics.jenkins.test.utils;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.domains.Domain;
import hudson.util.Secret;
import java.io.IOException;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;

public class CredentialsUtil {
  public static final String VALID_TOKEN = "test-token";

  private static final String CREDENTIAL_ID = "test-credential";

  /**
   * Create valid (as in accepted by MockServer) credentials to given Jenkins.
   *
   * @param jenkins Jenkins instance (can be test harness Jenkins)
   * @return Credential id
   * @throws IOException if adding credentials fails
   */
  public static String createValidCredentials(Jenkins jenkins) throws IOException {
    CredentialsStore store = CredentialsProvider.lookupStores(jenkins)
        .iterator()
        .next();
    StringCredentialsImpl credential = new StringCredentialsImpl(
        CredentialsScope.GLOBAL,
        CREDENTIAL_ID,
        "Test Secret Text",
        Secret.fromString(VALID_TOKEN));
    store.addCredentials(Domain.global(), credential);
    return CREDENTIAL_ID;
  }

}
