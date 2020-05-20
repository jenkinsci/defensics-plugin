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

import static org.apache.commons.lang.StringUtils.isNotBlank;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.domains.HostnameRequirement;
import hudson.security.ACL;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;


/**
 * Utility for Jenkins related functionality.
 */
public final class AuthenticationTokenProvider {

  private static final Logger LOGGER = Logger.getLogger(
      AuthenticationTokenProvider.class.getName());

  private AuthenticationTokenProvider() {
    // don't instantiate me
  }

  /**
   * Returns The first suitable credential for the id.
   *
   * @param url           Host to associate the credentials with
   * @param credentialsId The id of the credentials
   * @return Standard credential types
   */
  private static StringCredentials getCredentials(
      URL url,
      String credentialsId
  ) throws AuthenticationTokenNotFoundException {
    StringCredentials creds = CredentialsMatchers
        .firstOrNull(
            CredentialsProvider.lookupCredentials(
                StringCredentials.class,
                Jenkins.get(), ACL.SYSTEM,
                new HostnameRequirement(url.toExternalForm())),
            CredentialsMatchers.withId(credentialsId));
    LOGGER.log(Level.FINE, "Creds: {0}", creds);
    if (creds == null) {
      throw new AuthenticationTokenNotFoundException(
          "ERROR: Cannot find credential: '" + credentialsId + "'");
    }
    return creds;
  }

  /**
   * Returns The first suitable credential's plain text secret for the id.
   *
   * @param url           Host to associate the credentials with
   * @param credentialsId The id of the credentials
   * @return Standard credential types
   */
  public static String getAuthenticationToken(
      URL url,
      String credentialsId
  ) throws AuthenticationTokenNotFoundException {
    if (isNotBlank(credentialsId)) {
      StringCredentials creds = getCredentials(url, credentialsId);
      return creds.getSecret().getPlainText();
    } else {
      return "";
    }
  }
}
