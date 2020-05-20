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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

import hudson.model.Descriptor.FormException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class InstanceConfigurationValidatorTest {

  private InstanceConfigurationValidator instanceConfigurationValidator;
  private List<InstanceConfiguration> defensicsInstances;
  private InstanceConfiguration configuration;

  @Before
  public void setup() {
    instanceConfigurationValidator = new InstanceConfigurationValidator();
    defensicsInstances = new ArrayList();
    configuration = new InstanceConfiguration(
        "myDefensics",
        "http://my.defensics",
        true, "test-credential");
  }

  @Test
  public void testValidate() throws FormException {
    defensicsInstances.add(configuration);
    instanceConfigurationValidator.validate(defensicsInstances);
  }

  @Test
  public void testValidateNoConfigurations() throws FormException {
    instanceConfigurationValidator.validate(defensicsInstances);
  }

  @Test
  public void testValidateEmptyName() {
    InstanceConfiguration invalidConfiguration =
        new InstanceConfiguration("", "", true, "");
    defensicsInstances.add(configuration);
    defensicsInstances.add(invalidConfiguration);

    try {
      instanceConfigurationValidator.validate(defensicsInstances);
      fail("instanceConfigurationValidator.validate didn't throw FormException");
    } catch (FormException exception) {
      assertThat(
          exception.getMessage(),
          is(equalTo("Defensics instance name is empty")));
    }
  }

  @Test
  public void testValidateEmptyUrl() {
    InstanceConfiguration invalidConfiguration =
        new InstanceConfiguration("myDefensics", "", true, "test-credential");
    defensicsInstances.add(configuration);
    defensicsInstances.add(invalidConfiguration);
    try {
      instanceConfigurationValidator.validate(defensicsInstances);
      fail("instanceConfigurationValidator.validate didn't throw FormException");
    } catch (FormException exception) {
      assertThat(
          exception.getMessage(),
          is(equalTo("Defensics instance URL is empty")));
    }
  }

  @Test
  public void testValidateInvalidUrl() {
    String invalidUrl = "myInvalidUrl";
    InstanceConfiguration invalidConfiguration = new InstanceConfiguration(
        "myDefensics", invalidUrl, true, "test-credential");
    defensicsInstances.add(configuration);
    defensicsInstances.add(invalidConfiguration);
    try {
      instanceConfigurationValidator.validate(defensicsInstances);
      fail("instanceConfigurationValidator.validate didn't throw FormException");
    } catch (FormException exception) {
      assertThat(
          exception.getMessage(),
          is(equalTo("Defensics instance URL is not valid: " + invalidUrl)));
    }
  }

  @Test
  public void testValidateNonUniqueName() {
    defensicsInstances.add(configuration);
    defensicsInstances.add(configuration);
    try {
      instanceConfigurationValidator.validate(defensicsInstances);
      fail("instanceConfigurationValidator.validate didn't throw FormException");
    } catch (FormException exception) {
      assertThat(
          exception.getMessage(),
          is(equalTo(
              "The Defensics instance name is already configured: "
                  + configuration.getName())));
    }
  }
}
