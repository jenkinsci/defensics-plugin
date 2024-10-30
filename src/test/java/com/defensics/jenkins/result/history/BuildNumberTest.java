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

package com.defensics.jenkins.result.history;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Before;
import org.junit.Test;

public class BuildNumberTest {

  public static final int BUILD_NUMBER = 123;

  private BuildNumber buildNumber;

  @Before
  public void setup() {
    buildNumber = new BuildNumber(BUILD_NUMBER);
  }

  @Test
  public void testCreation() {
    assertThat(buildNumber, is(notNullValue()));
  }

  @Test
  public void testGetBuildNumber() {
    assertThat(buildNumber.getBuildNumber(), is(equalTo(BUILD_NUMBER)));
  }

  @Test
  public void testEqualsAndHashCode() {
    EqualsVerifier.forClass(BuildNumber.class).verify();
  }

  @Test
  public void testToString() {
    assertThat(buildNumber.toString(), is(equalTo("#123")));
  }
}
