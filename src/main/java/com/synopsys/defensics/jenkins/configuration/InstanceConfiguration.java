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

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.synopsys.defensics.api.ApiService;
import com.synopsys.defensics.client.DefensicsRequestException;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

/**
 * Global configuration for one Defensics Instance.
 */
public class InstanceConfiguration extends
    AbstractDescribableImpl<InstanceConfiguration> {

  private final String url;
  private final String name;
  private final boolean certificateValidationDisabled;
  private final String credentialsId;

  /**
   * Constructor that gets parameters from Jenkins.
   *
   * @param name                          Name of the Defensics instance.
   * @param url                           URL of the Defensics instance.
   * @param certificateValidationDisabled Is certificate validation disabled when communicating with
   *                                      Defensics.
   * @param credentialsId                 Credentials id for the Defensics instance.
   */
  @DataBoundConstructor
  public InstanceConfiguration(String name, String url, boolean certificateValidationDisabled,
      String credentialsId) {
    this.name = name;
    this.url = url;
    this.certificateValidationDisabled = certificateValidationDisabled;
    this.credentialsId = credentialsId;
  }

  public String getName() {
    return name;
  }

  public String getUrl() {
    return url;
  }

  public boolean isCertificateValidationDisabled() {
    return certificateValidationDisabled;
  }

  public String getCredentialsId() {
    return credentialsId;
  }

  public String getDisplayName() {
    return getName() + " (" + getUrl() + ")";
  }

  @Override
  public final boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof InstanceConfiguration)) {
      return false;
    }
    InstanceConfiguration that = (InstanceConfiguration) o;
    return Objects.equals(url, that.url)
        && Objects.equals(name, that.name)
        && certificateValidationDisabled == that.certificateValidationDisabled
        && Objects.equals(credentialsId, that.credentialsId);
  }

  @Override
  public final int hashCode() {
    return Objects.hash(url, name, certificateValidationDisabled, credentialsId);
  }

  @Extension
  public static class DefensicsInstanceConfigurationDescriptor extends
      Descriptor<InstanceConfiguration> {

    /**
     * Validation for Defensics instance name.
     *
     * @param name The name the user has entered.
     * @return Ok if name is valid, otherwise Error.
     */
    public FormValidation doCheckName(@QueryParameter final String name) {
      if (isNotBlank(name)) {
        return FormValidation.ok();
      } else {
        return FormValidation.error("Name is not defined");
      }
    }

    /**
     * Fills Credentials drop down menu with the StringCredentials description field values.
     *
     * @param item          nearest ancestor item in Jenkins hierarchy.
     * @param credentialsId currently selected credentials (can be null).
     * @return
     */
    public ListBoxModel doFillCredentialsIdItems(
        @AncestorInPath Item item,
        @QueryParameter String credentialsId) {
      StandardListBoxModel result = new StandardListBoxModel();
      if (item == null) {
        if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
          return result.includeCurrentValue(credentialsId);
        }
      } else {
        if (!item.hasPermission(Item.EXTENDED_READ)
            && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
          return result.includeCurrentValue(credentialsId);
        }
      }
      return result.includeEmptyValue()
          .includeAs(ACL.SYSTEM, Jenkins.get(),
              StringCredentials.class)
          .includeCurrentValue(credentialsId);
    }

    /**
     * Check credential drop down list.
     *
     * @param item  nearest ancestor item in Jenkins hierarchy.
     * @param value value of credentials id.
     * @return
     */
    public FormValidation doCheckCredentialsId(
        @AncestorInPath Item item,
        @QueryParameter String value) {
      if (item == null) {
        if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
          return FormValidation.ok();
        }
      } else {
        if (!item.hasPermission(Item.EXTENDED_READ)
            && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
          return FormValidation.ok();
        }
      }
      if (StringUtils.isBlank(value)) {
        return FormValidation.ok();
      }
      if (value.startsWith("${") && value.endsWith("}")) {
        return FormValidation.warning("Cannot validate expression based credentials");
      }

      return FormValidation.ok();
    }

    /**
     * Validation for Defensics instance URL.
     *
     * @param url The url the user has entered.
     * @return Ok if the url is set and is a valid url, otherwise Error.
     */
    public FormValidation doCheckUrl(@QueryParameter final String url) {

      if (isNotBlank(url)) {
        try {
          new URL(url);
          return FormValidation.ok();
        } catch (MalformedURLException e) {
          return FormValidation.error(e.getMessage());
        }
      } else {
        return FormValidation.error("URL is not defined");
      }
    }

    /**
     * Test the connection to Defensics instance.
     *
     * @param url           The URL of the Defensics instance to connect to.
     * @param credentialsId Id of the authorization token-credential to be used (selected from the
     *                      drop down menu).
     * @return Ok if a get request is successfully made to the api endpoint of the url, otherwise
     *         Error.
     */
    @POST
    public FormValidation doTestConnection(@QueryParameter("url") final String url,
        @QueryParameter("certificateValidationDisabled") final boolean
            certificateValidationDisabled,
        @QueryParameter("credentialsId") final String credentialsId) {
      try {
        ApiService apiService = new ApiService(
            url,
            AuthenticationTokenProvider.getAuthenticationToken(new URL(url), credentialsId),
            certificateValidationDisabled);
        apiService.healthCheck();
        return FormValidation.ok("Success");
      } catch (AuthenticationTokenNotFoundException e) {
        return FormValidation.error(e.getMessage());
      } catch (InterruptedException| IOException | DefensicsRequestException e) {
        return FormValidation.error("Failed to connect to server: " + e.getMessage());
      }
    }
  }
}
