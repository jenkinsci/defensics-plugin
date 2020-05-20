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

package com.synopsys.defensics.jenkins;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import hudson.EnvVars;
import hudson.model.TaskListener;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PollingIntervalsTest {

  @Mock private hudson.model.Run<?, ?> jenkinsRun;
  @Mock private TaskListener listener;
  @Mock private Logger logger;

  PollingIntervals pollingIntervals;

  private void setup(EnvVars environmentVariables) throws Exception {
    when(jenkinsRun.getEnvironment(eq(listener))).thenReturn(environmentVariables);
    pollingIntervals = new PollingIntervals(jenkinsRun, listener, logger);
  }

  @Test
  public void testGetTestplanLoadingIntervalNoMaximum() throws Exception {
    setup(new EnvVars());

    assertThat(pollingIntervals.getTestplanLoadingInterval(), is(equalTo(5)));
  }

  @Test
  public void testGetInitialRunPollingIntervalNoMaximum() throws Exception {
    setup(new EnvVars());

    assertThat(pollingIntervals.getInitialRunPollingInterval(), is(equalTo(5)));
  }

  @Test
  public void testGetRunPollingIntervalNoMaximum() throws Exception {
    setup(new EnvVars());

    assertThat(pollingIntervals.getRunPollingInterval(), is(equalTo(30)));
  }

  @Test
  public void testMaximumIntervalLarge() throws Exception {
    EnvVars environmentVariables = new EnvVars();
    environmentVariables.put("DEFENSICS_MAX_POLLING_INTERVAL", "60");
    setup(environmentVariables);

    assertThat(pollingIntervals.getTestplanLoadingInterval(), is(equalTo(5)));
    assertThat(pollingIntervals.getInitialRunPollingInterval(), is(equalTo(5)));
    assertThat(pollingIntervals.getRunPollingInterval(), is(equalTo(30)));
  }

  @Test
  public void testMaximumIntervalMedium() throws Exception {
    EnvVars environmentVariables = new EnvVars();
    environmentVariables.put("DEFENSICS_MAX_POLLING_INTERVAL", "10");
    setup(environmentVariables);

    assertThat(pollingIntervals.getTestplanLoadingInterval(), is(equalTo(5)));
    assertThat(pollingIntervals.getInitialRunPollingInterval(), is(equalTo(5)));
    assertThat(pollingIntervals.getRunPollingInterval(), is(equalTo(10)));
  }

  @Test
  public void testMaximumIntervalSmall() throws Exception {
    EnvVars environmentVariables = new EnvVars();
    environmentVariables.put("DEFENSICS_MAX_POLLING_INTERVAL", "1");
    setup(environmentVariables);

    assertThat(pollingIntervals.getTestplanLoadingInterval(), is(equalTo(1)));
    assertThat(pollingIntervals.getInitialRunPollingInterval(), is(equalTo(1)));
    assertThat(pollingIntervals.getRunPollingInterval(), is(equalTo(1)));
  }

  @Test
  public void testMaximumIntervalZero() throws Exception {
    EnvVars environmentVariables = new EnvVars();
    environmentVariables.put("DEFENSICS_MAX_POLLING_INTERVAL", "0");
    setup(environmentVariables);

    assertThat(pollingIntervals.getTestplanLoadingInterval(), is(equalTo(0)));
    assertThat(pollingIntervals.getInitialRunPollingInterval(), is(equalTo(0)));
    assertThat(pollingIntervals.getRunPollingInterval(), is(equalTo(0)));
  }

  @Test
  public void testMaximumIntervalInvalid() throws Exception {
    EnvVars environmentVariables = new EnvVars();
    environmentVariables.put("DEFENSICS_MAX_POLLING_INTERVAL", "not_a_number");
    setup(environmentVariables);

    assertThat(pollingIntervals.getTestplanLoadingInterval(), is(equalTo(5)));
    assertThat(pollingIntervals.getInitialRunPollingInterval(), is(equalTo(5)));
    assertThat(pollingIntervals.getRunPollingInterval(), is(equalTo(30)));
  }
}